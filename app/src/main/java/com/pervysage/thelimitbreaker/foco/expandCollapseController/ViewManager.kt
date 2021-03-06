package com.pervysage.thelimitbreaker.foco.expandCollapseController

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.ViewTreeObserver
import com.pervysage.thelimitbreaker.foco.database.entities.PlacePrefs
import java.text.FieldPosition

class ViewManager(private val listView:MyListView){

    // value to indicate pass of preDraw
    private var preDrawPass=0

    // coordinates of changed view
    private var translateCoords = IntArray(2)

    fun expand(viewToExpand:View,doWorkInExpand:()->Unit,doWorkInCollapse:(()->Unit)?){

        listView.isScrollEnabled=false

        var oldTop = 0
        var oldBottom = 0
        val oldCoordinates = HashMap<View, IntArray>()

        val prepareExpand={
            preDrawPass=1
            oldTop=viewToExpand.top
            oldBottom=viewToExpand.bottom
            for (i in 0 until listView.childCount){
                val v = listView.getChildAt(i)
                oldCoordinates[v]= intArrayOf(v.top,v.bottom)
            }
            doWorkInExpand()
        }

        doWorkInCollapse?.invoke()?:prepareExpand()

        viewToExpand.viewTreeObserver.addOnPreDrawListener(object :ViewTreeObserver.OnPreDrawListener{
            override fun onPreDraw(): Boolean {
                return when(preDrawPass){
                    0->{
                        prepareExpand()
                        listView.requestLayout()
                       false
                    }
                    1->{
                        preDrawPass=2
                        val newTop = viewToExpand.top
                        val newBottom = viewToExpand.bottom
                        val oldHeight = oldBottom - oldTop
                        val newHeight = newBottom - newTop
                        val delta = newHeight - oldHeight

                        translateCoords = getExpandedTopBottom(oldTop, oldBottom, delta)
                        listView.requestLayout()
                        false
                    }
                    else->{
                        preDrawPass=0
                        viewToExpand.viewTreeObserver.removeOnPreDrawListener(this)

                        val animations = ArrayList<Animator>()

                        val yTranslateTop = translateCoords[0]
                        val yTranslateBottom = translateCoords[1]
                        val index = listView.indexOfChild(viewToExpand)
                        for (v in oldCoordinates.keys) {

                            val i = listView.indexOfChild(v)
                            val oldChildTopBottom = oldCoordinates[v]
                            v.top = oldChildTopBottom!![0]
                            v.bottom = oldChildTopBottom[1]
                            if (v != viewToExpand) {
                                val delta =
                                        if (i > index){
                                            yTranslateBottom
                                        }
                                        else {
                                            -yTranslateTop
                                        }
                                animations.add(getAnimation(v, delta * 1f, delta * 1f))
                            }

                        }

                        val selViewTop = viewToExpand.top
                        val selViewBottom = viewToExpand.bottom
                        val selViewEndTop = selViewTop + (-yTranslateTop)
                        val selViewEndBottom = selViewBottom + yTranslateBottom

                        val expandTopAnim = ObjectAnimator.ofInt(
                                viewToExpand,
                                "top",
                                selViewTop, selViewEndTop
                        )


                        expandTopAnim.duration = 250
                        val expandBottomAnim = ObjectAnimator.ofInt(
                                viewToExpand,
                                "bottom",
                                selViewBottom, selViewEndBottom
                        )
                        expandBottomAnim.duration = 250
                        animations.add(expandTopAnim)
                        animations.add(expandBottomAnim)
                        val animatorSet = AnimatorSet()
                        animatorSet.playTogether(animations)
                        animatorSet.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {// TODO Not Implemented
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                listView.isScrollEnabled=true
                            }

                            override fun onAnimationCancel(animation: Animator?) {// TODO Not Implemented
                            }

                            override fun onAnimationStart(animation: Animator?) {// TODO Not Implemented
                            }

                        })
                        animatorSet.start()
                        true
                    }

                }
            }

        })
    }

    fun collapse(viewToCollapse:View,doWorkInCollapse: () -> Unit){
        listView.isScrollEnabled=false

        val oldTop = viewToCollapse.top
        val oldBottom = viewToCollapse.bottom

        val oldCoordinates = HashMap<View, IntArray>()
        for (i in 0 until listView.childCount) {
            val child = listView.getChildAt(i)
            val oldTopBottom = IntArray(2)
            oldTopBottom[0] = child!!.top
            oldTopBottom[1] = child.bottom
            oldCoordinates[child] = oldTopBottom
        }
        doWorkInCollapse()
        viewToCollapse.viewTreeObserver.addOnPreDrawListener(object :ViewTreeObserver.OnPreDrawListener{
            override fun onPreDraw(): Boolean {
                if (preDrawPass==0){
                    preDrawPass=1
                    val newTop = viewToCollapse.top
                    val newBottom = viewToCollapse.bottom

                    translateCoords[0] = newTop - oldTop
                    translateCoords[1] = newBottom - oldBottom
                    listView.requestLayout()
                    return false
                }
                preDrawPass=0
                viewToCollapse.viewTreeObserver.removeOnPreDrawListener(this)

                val animations = ArrayList<Animator>()

                val yTranslateTop = translateCoords[0]
                val yTranslateBottom = translateCoords[1]

                val index = listView.indexOfChild(viewToCollapse)

                for (i in 0 until listView.childCount) {
                    val v = listView.getChildAt(i)

                    val oldTopBottom = oldCoordinates[v]
                    if (oldTopBottom != null) {
                        v.top = oldTopBottom[0]
                        v.bottom = oldTopBottom[1]
                    } else {
                        val delta = if (i > index) -yTranslateBottom else yTranslateTop
                        v.top = if (v.top < 0 || i<index) v.top - delta else v.top + delta
                        v.bottom = if (v.top < 0 || i < index) v.bottom - delta else v.bottom + delta
                    }
                    if (v != viewToCollapse) {

                        val diff = if (i > index) yTranslateBottom else yTranslateTop
                        animations.add(getAnimation(v, diff * 1f, diff * 1f))
                    }
                }
                val selViewTop = viewToCollapse.top
                val selViewBottom = viewToCollapse.bottom
                val selViewEndTop = selViewTop + yTranslateTop
                val selViewEndBottom = selViewBottom + yTranslateBottom

                val collapseTopAnim = ObjectAnimator.ofInt(
                        viewToCollapse,
                        "top",
                        selViewTop, selViewEndTop
                )


                collapseTopAnim.duration = 150
                val collapseBottomAnim = ObjectAnimator.ofInt(
                        viewToCollapse,
                        "bottom",
                        selViewBottom, selViewEndBottom
                )
                collapseBottomAnim.duration = 150
                animations.add(collapseTopAnim)
                animations.add(collapseBottomAnim)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(animations)
                animatorSet.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {// TODO Not Implemented
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        listView.isScrollEnabled=true

                    }

                    override fun onAnimationCancel(animation: Animator?) {// TODO Not Implemented
                    }

                    override fun onAnimationStart(animation: Animator?) {// TODO Not Implemented
                    }

                })
                animatorSet.start()
                return true
            }

        })
    }


    private fun getAnimation(view: View, translateTop: Float, translateBottom: Float): Animator {

        val top = view.top
        val bottom = view.bottom
        val endTop = (top + translateTop).toInt()
        val endBottom = (bottom + translateBottom).toInt()

        val translationTop = PropertyValuesHolder.ofInt("top", top, endTop)
        val translationBottom = PropertyValuesHolder.ofInt("bottom", bottom, endBottom)

        val anim = ObjectAnimator.ofPropertyValuesHolder(view, translationTop, translationBottom)
        anim.duration = 250
        return anim
    }

    private fun getExpandedTopBottom(top: Int, bottom: Int, yDelta: Int): IntArray {
        var yTranslateTop = 0
        var yTranslateBottom = yDelta

        val height = bottom - top

        val isOverTop = top < 0
        val isBelowBottom = top + height + yDelta > (listView.height-200)
        if (isOverTop) {
            yTranslateTop = top
            yTranslateBottom = yDelta - yTranslateTop
        } else if (isBelowBottom) {
            val deltaBelow = top + height + yDelta - (listView.height-200)
            yTranslateTop = if (top - deltaBelow < 0) top else deltaBelow
            yTranslateBottom = yDelta - yTranslateTop
        }

        return intArrayOf(yTranslateTop, yTranslateBottom)
    }


}