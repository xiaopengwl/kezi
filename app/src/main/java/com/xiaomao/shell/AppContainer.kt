package com.xiaomao.shell

import android.content.Context
import com.xiaomao.shell.data.RuleRepository
import com.xiaomao.shell.data.SourceStore
import com.xiaomao.shell.runtime.DrpyBridge
import com.xiaomao.shell.runtime.DrpyWebViewEngine

object AppContainer {
    @Volatile
    private var repository: RuleRepository? = null

    fun repository(context: Context): RuleRepository {
        return repository ?: synchronized(this) {
            repository ?: buildRepository(context.applicationContext).also { repository = it }
        }
    }

    private fun buildRepository(context: Context): RuleRepository {
        val client = DrpyBridge.defaultClient()
        val bridge = DrpyBridge(context, client)
        val engine = DrpyWebViewEngine(context, bridge)
        val sourceStore = SourceStore(context)
        return RuleRepository(sourceStore, engine, client)
    }
}
