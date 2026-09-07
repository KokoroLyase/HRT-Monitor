package com.hrt.monitor.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 应用数据的 JSON 序列化（备份/恢复格式） */
object JsonIO {

    fun stringify(d: AppData): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("records", JSONArray().apply {
            d.records.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("date", r.dateMillis)
                    put("note", r.note)
                    put("items", JSONArray().apply {
                        r.items.forEach { m ->
                            put(JSONObject().apply {
                                put("h", m.hormone.key)
                                put("v", m.value)
                                put("u", m.unit)
                            })
                        }
                    })
                })
            }
        })
        root.put("events", JSONArray().apply {
            d.events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("date", e.dateMillis)
                    put("type", e.type.key)
                    put("note", e.note)
                })
            }
        })
        root.put("ranges", JSONArray().apply {
            d.ranges.filter { !it.builtin }.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("h", r.hormone.key)
                    put("label", r.label)
                    put("min", r.min)
                    put("max", r.max)
                    put("unit", r.unit)
                    put("visible", r.visible)
                })
            }
        })
        root.put("targets", JSONObject().apply {
            d.settings.targets.forEach { (k, v) -> put(k, v) }
        })
        return root.toString(2)
    }

    fun parse(text: String): AppData? = try {
        val root = JSONObject(text)
        val records = mutableListOf<LabRecord>()
        val recArr = root.optJSONArray("records")
        if (recArr != null) {
            for (i in 0 until recArr.length()) {
                try {
                    val o = recArr.getJSONObject(i)
                    val items = mutableListOf<Measurement>()
                    val itArr = o.optJSONArray("items")
                    if (itArr != null) {
                        for (j in 0 until itArr.length()) {
                            val m = itArr.getJSONObject(j)
                            val h = Hormone.byKey(m.optString("h"))
                            val v = m.optDouble("v")
                            if (h != null && v.isFinite() && v > 0) {
                                items.add(Measurement(h, v, m.optString("u", "")))
                            }
                        }
                    }
                    records.add(
                        LabRecord(
                            id = o.optString("id", "").ifEmpty { UUID.randomUUID().toString() },
                            dateMillis = o.optLong("date", 0L),
                            note = o.optString("note", ""),
                            items = items
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
        val events = mutableListOf<LifeEvent>()
        val evArr = root.optJSONArray("events")
        if (evArr != null) {
            for (i in 0 until evArr.length()) {
                try {
                    val o = evArr.getJSONObject(i)
                    events.add(
                        LifeEvent(
                            id = o.optString("id", "").ifEmpty { UUID.randomUUID().toString() },
                            dateMillis = o.optLong("date", 0L),
                            type = EventType.byKey(o.optString("type")) ?: EventType.OTHER,
                            note = o.optString("note", "")
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
        val ranges = mutableListOf<RefRange>()
        val rgArr = root.optJSONArray("ranges")
        if (rgArr != null) {
            for (i in 0 until rgArr.length()) {
                try {
                    val o = rgArr.getJSONObject(i)
                    val h = Hormone.byKey(o.optString("h")) ?: continue
                    val min = o.optDouble("min")
                    val max = o.optDouble("max")
                    if (!min.isFinite() || !max.isFinite()) continue
                    ranges.add(
                        RefRange(
                            id = o.optString("id", "").ifEmpty { UUID.randomUUID().toString() },
                            hormone = h,
                            label = o.optString("label", "").ifEmpty { null },
                            min = min,
                            max = max,
                            unit = o.optString("unit", ""),
                            builtin = false,
                            visible = o.optBoolean("visible", true)
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
        val targets = mutableMapOf<String, String>()
        val tObj = root.optJSONObject("targets")
        if (tObj != null) {
            tObj.keys().forEach { k -> targets[k] = tObj.optString(k) }
        }
        AppData(records, events, ranges, Settings(targets))
    } catch (_: Exception) {
        null
    }
}
