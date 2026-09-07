package com.hrt.monitor.data

/**
 * 内置参考范围。
 * 来源：用户提供的参考数据 + 常见公开实验室参考值。
 * 仅供参考、非诊断依据；用户可在应用中自定义并设置目标范围。
 */
object SeedData {

    fun builtinRanges(): List<RefRange> = listOf(
        // —— 雌二醇 E2（pg/mL）——
        RefRange("builtin-e2-male", Hormone.E2, labelRes = "range_e2_male",
            min = 8.0, max = 35.0, unit = "pg/mL", builtin = true, visible = false),
        RefRange("builtin-e2-foll", Hormone.E2, labelRes = "range_e2_foll",
            min = 30.0, max = 100.0, unit = "pg/mL", builtin = true, visible = true),
        RefRange("builtin-e2-lut", Hormone.E2, labelRes = "range_e2_lut",
            min = 70.0, max = 300.0, unit = "pg/mL", builtin = true, visible = true),
        RefRange("builtin-e2-gaht", Hormone.E2, labelRes = "range_e2_gaht",
            min = 100.0, max = 200.0, unit = "pg/mL", builtin = true, visible = true),

        // —— 睾酮 T（ng/mL）——
        RefRange("builtin-t-male", Hormone.T, labelRes = "range_t_male",
            min = 2.64, max = 9.16, unit = "ng/mL", builtin = true, visible = false),
        RefRange("builtin-t-female", Hormone.T, labelRes = "range_t_female",
            min = 0.1, max = 0.55, unit = "ng/mL", builtin = true, visible = true),
        RefRange("builtin-t-gaht", Hormone.T, labelRes = "range_t_gaht",
            min = 0.0, max = 0.55, unit = "ng/mL", builtin = true, visible = true),

        // —— 催乳素 PRL（ng/mL）——
        RefRange("builtin-prl-female", Hormone.PRL, labelRes = "range_prl_female",
            min = 4.79, max = 23.3, unit = "ng/mL", builtin = true, visible = true),
        RefRange("builtin-prl-high", Hormone.PRL, labelRes = "range_prl_high",
            min = 69.9, max = 69.9, unit = "ng/mL", builtin = true, visible = true,
            thresholdOnly = true),

        // —— 促黄体生成素 LH（IU/L）——
        RefRange("builtin-lh-foll", Hormone.LH, labelRes = "range_lh_foll",
            min = 2.12, max = 10.89, unit = "IU/L", builtin = true, visible = true),
        RefRange("builtin-lh-ovu", Hormone.LH, labelRes = "range_lh_ovu",
            min = 19.18, max = 103.03, unit = "IU/L", builtin = true, visible = false),
        RefRange("builtin-lh-lut", Hormone.LH, labelRes = "range_lh_lut",
            min = 1.20, max = 12.86, unit = "IU/L", builtin = true, visible = true),
        RefRange("builtin-lh-gaht", Hormone.LH, labelRes = "range_lh_gaht",
            min = 0.0, max = 2.0, unit = "IU/L", builtin = true, visible = true),

        // —— 促卵泡生成激素 FSH（IU/L）——
        RefRange("builtin-fsh-foll", Hormone.FSH, labelRes = "range_fsh_foll",
            min = 3.85, max = 8.78, unit = "IU/L", builtin = true, visible = true),
        RefRange("builtin-fsh-ovu", Hormone.FSH, labelRes = "range_fsh_ovu",
            min = 4.54, max = 22.51, unit = "IU/L", builtin = true, visible = false),
        RefRange("builtin-fsh-lut", Hormone.FSH, labelRes = "range_fsh_lut",
            min = 1.79, max = 5.12, unit = "IU/L", builtin = true, visible = true),
        RefRange("builtin-fsh-gaht", Hormone.FSH, labelRes = "range_fsh_gaht",
            min = 0.0, max = 2.0, unit = "IU/L", builtin = true, visible = true),

        // —— 孕酮 P4（nmol/L）——
        RefRange("builtin-p4-foll", Hormone.P4, labelRes = "range_p4_foll",
            min = 0.99, max = 4.83, unit = "nmol/L", builtin = true, visible = true),
        RefRange("builtin-p4-lut", Hormone.P4, labelRes = "range_p4_lut",
            min = 16.4, max = 59.0, unit = "nmol/L", builtin = true, visible = false),
        RefRange("builtin-p4-gaht", Hormone.P4, labelRes = "range_p4_gaht",
            min = 0.0, max = 3.18, unit = "nmol/L", builtin = true, visible = true)
    )

    /** 各激素的默认目标范围（可在「参考范围管理」中更改） */
    fun defaultTargets(): Map<String, String> = mapOf(
        "e2" to "builtin-e2-gaht",
        "t" to "builtin-t-gaht",
        "prl" to "builtin-prl-female",
        "lh" to "builtin-lh-gaht",
        "fsh" to "builtin-fsh-gaht",
        "p4" to "builtin-p4-gaht"
    )
}
