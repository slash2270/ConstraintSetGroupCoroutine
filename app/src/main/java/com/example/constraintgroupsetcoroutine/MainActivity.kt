package com.example.constraintgroupsetcoroutine

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.transition.TransitionManager
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    private lateinit var strBuffer: StringBuffer

    private lateinit var strContext: String

    private val scopeWork = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = Job()
    }

    private val progress: ProgressBar by lazy {
        findViewById(R.id.progress)
    }

    private val tvGlobal: TextView by lazy {
        findViewById(R.id.tvGlobal)
    }

    private val tvContext1: TextView by lazy {
        findViewById(R.id.tvContext1)
    }

    private val tvContext2: TextView by lazy {
        findViewById(R.id.tvContext2)
    }

    private val tvContext3: TextView by lazy {
        findViewById(R.id.tvContext3)
    }

    private val tvRunBlocking1: TextView by lazy {
        findViewById(R.id.tvRunBlocking1)
    }

    private val tvRunBlocking2: TextView by lazy {
        findViewById(R.id.tvRunBlocking2)
    }

    private val tvAsync1: TextView by lazy {
        findViewById(R.id.tvAsync1)
    }

    private val tvAsync2: TextView by lazy {
        findViewById(R.id.tvAsync2)
    }

    private val tvAsync3: TextView by lazy {
        findViewById(R.id.tvAsync3)
    }

    private val groupAsync: Group by lazy {
        findViewById(R.id.groupAsync)
    }

    private val strAsync: Deferred<String> by lazy { // 緩徵 deferred保護 必須搭配await使用
        scopeWork.async(Dispatchers.IO) { // 並聯 照最後await的速度回傳
            "Async"
        }
    }

    private val strRunBlocking: String by lazy { // 串聯 照結果速度回傳
        "RunBlocking"
    }

    private val constraintSet: ConstraintSet by lazy {
        ConstraintSet()
    }

    private val constraintLayout: ConstraintLayout by lazy {
        findViewById(R.id.constraint)
    }

    private fun setConstraint() { // 必須一開始init在UIThread 如果是接值或visible會直接跳到View2
        constraintSet.clone(this, R.layout.activity_main_constraintset) // 鎖定在Set的View
        strContext = "Context"
        strBuffer = StringBuffer(strContext).append(2)
        postText(tvContext2, strBuffer.toString())
        strBuffer = StringBuffer(strContext).append(3)
        postText(tvContext3, strBuffer.toString())
    }

    private fun setConstraintSet() {
        TransitionManager.beginDelayedTransition(constraintLayout)
        constraintSet.applyTo(constraintLayout)
    }

    private val jobGlobal: Job by lazy {
        MainScope().launch(Dispatchers.Main) { // or GlobalScope() 都是繼承於CoroutineScope 靜態函數用法
            setConstraint()
            // 只要在同個launch執行都是照排序
            groupAsync.visibility = View.GONE
            for (i in 5 downTo 1) {
                getGlobal(i)
                delay(1000)
                setWithContext()
            }
            // async thread
            scopeWork.launch {
                setAsync(5000, "1")
                postText(tvAsync1, strBuffer.toString())
            }
            scopeWork.launch { // async thread
                setAsync(5000, "2")
                postText(tvAsync2, strBuffer.toString())
            }
            scopeWork.launch { // async thread
                setAsync(5000, "3")
                postText(tvAsync3, strBuffer.toString())
            }
            setConstraintSet()
            progress.visibility = View.GONE
            groupAsync.visibility = View.VISIBLE
            setGlobal()
            getWithContext()
            // runBlocking thread
            scopeWork.launch {
                setRunBlocking(4000, 1)
                postText(tvRunBlocking1, strBuffer.toString())
            }
            scopeWork.launch {
                setRunBlocking(2000, 2)
                postText(tvRunBlocking2, strBuffer.toString())
            }
        }
    }

    private fun getGlobal(time: Int) {
        strBuffer = StringBuffer()
        tvGlobal.text = strBuffer.append("倒數").append(" ").append("$time").append(" ").append("...") // update text
    }

    private fun setGlobal() {
        strBuffer = StringBuffer("Hi")
        tvGlobal.text = strBuffer
    }

    private suspend fun setWithContext() = withContext(Dispatchers.IO) {
        strContext = "Context1"
    }

    private fun getWithContext(){
        postText(tvContext1, strContext)
    }

    private suspend fun setAsync(timeMillis: Long, quantity: String) { // await等待
        delay(timeMillis)
        strBuffer = StringBuffer(strAsync.await()).append(quantity)
    }

    private fun setRunBlocking(timeMillis: Long, quantity: Int) {
        runBlocking(Dispatchers.IO) {
            delay(timeMillis)
            strBuffer = StringBuffer(strRunBlocking).append(quantity)
        }
    }

    private fun postText(textView: TextView, string: String) {
        textView.post {
            textView.text = string
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        jobGlobal.start()
    }

    override fun onDestroy() {
        jobGlobal.cancel()
        scopeWork.cancel()
        super.onDestroy()
    }

}