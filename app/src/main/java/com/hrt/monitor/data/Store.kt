package com.hrt.monitor.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 应用数据仓库：内存状态 + 本地 JSON 文件持久化。
 * 所有数据仅保存在应用私有目录，无任何网络上传。
 */
object AppRepo {
    private const val FILE_NAME = "hrt_data.json"
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, FILE_NAME)
        file = f
        scope.launch {
            val loaded = try {
                f.readText().takeIf { it.isNotBlank() }?.let { JsonIO.parse(it) }
            } catch (_: Exception) {
                null
            }
            val base = loaded ?: AppData()
            val builtins = SeedData.builtinRanges()
            val ids = base.ranges.map { it.id }.toSet()
            val ranges = base.ranges + builtins.filter { it.id !in ids }
            val targets = SeedData.defaultTargets() + base.settings.targets
            _data.value = base.copy(ranges = ranges, settings = base.settings.copy(targets = targets))
            if (loaded == null) persist()
        }
    }

    // —— 化验记录 ——
    fun upsertRecord(record: LabRecord) = mutate { d ->
        val list = d.records.filter { it.id != record.id } + record
        d.copy(records = list.sortedByDescending { it.dateMillis })
    }

    fun deleteRecord(id: String) = mutate { d ->
        d.copy(records = d.records.filter { it.id != id })
    }

    // —— 生活事件 ——
    fun upsertEvent(event: LifeEvent) = mutate { d ->
        val list = d.events.filter { it.id != event.id } + event
        d.copy(events = list.sortedByDescending { it.dateMillis })
    }

    fun deleteEvent(id: String) = mutate { d ->
        d.copy(events = d.events.filter { it.id != id })
    }

    // —— 参考范围 ——
    fun upsertRange(range: RefRange) = mutate { d ->
        d.copy(ranges = d.ranges.filter { it.id != range.id } + range)
    }

    fun deleteRange(id: String) = mutate { d ->
        d.copy(
            ranges = d.ranges.filter { it.id != id },
            settings = d.settings.copy(targets = d.settings.targets.filterValues { it != id })
        )
    }

    fun setRangeVisible(id: String, visible: Boolean) = mutate { d ->
        d.copy(ranges = d.ranges.map { if (it.id == id) it.copy(visible = visible) else it })
    }

    fun setTarget(hormone: Hormone, rangeId: String?) = mutate { d ->
        val targets = if (rangeId == null) {
            d.settings.targets - hormone.key
        } else {
            d.settings.targets + (hormone.key to rangeId)
        }
        d.copy(settings = d.settings.copy(targets = targets))
    }

    // —— 导入 / 清空 ——
    data class ImportResult(val records: Int, val events: Int, val ranges: Int)

    fun import(parsed: AppData, replace: Boolean): ImportResult {
        var result = ImportResult(0, 0, 0)
        mutate { d ->
            val builtins = SeedData.builtinRanges()
            if (replace) {
                val ids = parsed.ranges.map { it.id }.toSet()
                val ranges = parsed.ranges + builtins.filter { it.id !in ids }
                val targets = SeedData.defaultTargets() + parsed.settings.targets
                result = ImportResult(parsed.records.size, parsed.events.size, parsed.ranges.size)
                d.copy(
                    records = parsed.records.sortedByDescending { it.dateMillis },
                    events = parsed.events.sortedByDescending { it.dateMillis },
                    ranges = ranges,
                    settings = Settings(targets)
                )
            } else {
                val records = (d.records + parsed.records).associateBy { it.id }.values
                    .sortedByDescending { it.dateMillis }
                val events = (d.events + parsed.events).associateBy { it.id }.values
                    .sortedByDescending { it.dateMillis }
                val customRanges = (d.ranges.filter { !it.builtin } + parsed.ranges)
                    .associateBy { it.id }.values
                val rangeIds = customRanges.map { it.id }.toSet()
                val ranges = customRanges + builtins.filter { it.id !in rangeIds }
                val targets = SeedData.defaultTargets() + d.settings.targets + parsed.settings.targets
                result = ImportResult(parsed.records.size, parsed.events.size, parsed.ranges.size)
                d.copy(
                    records = records,
                    events = events,
                    ranges = ranges,
                    settings = Settings(targets)
                )
            }
        }
        return result
    }

    fun clearAll() = mutate {
        it.copy(
            records = emptyList(),
            events = emptyList(),
            ranges = SeedData.builtinRanges(),
            settings = Settings(SeedData.defaultTargets())
        )
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun mutate(transform: (AppData) -> AppData) {
        _data.update { transform(it) }
        persist()
    }

    private fun persist() {
        val f = file ?: return
        val text = JsonIO.stringify(_data.value)
        scope.launch {
            try {
                val tmp = File(f.parentFile, FILE_NAME + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(f)) {
                    f.writeText(text)
                    tmp.delete()
                }
            } catch (_: Exception) {
            }
        }
    }
}
