package com.example.config

data class DropboxConfig(
    val accessToken: String,
    val appKey: String,
    val appSecret: String,
    val basePath: String = "/school-management"
) {
    companion object {
        fun fromEnvironment(): DropboxConfig {
            return DropboxConfig(
                accessToken = System.getenv("DROPBOX_ACCESS_TOKEN") ?:"sl.u.AF20a-PzlHNVcJgRosDPkvu1XFFIzf6oK4Ld0hz_sturzbCvTA-Ivu5HpxoYzQF6cMy88JdAse1Of5dh9Te0Ob8x247ydUf-ZTv5_FTvtXJIQTDeQCZsR9BEvBFfHPmmMTBq95DJRAOeOf-6Aay_021G8DfA4EAF5Ckfp-uAiCwiW9s8hAHP8DG6acCmQ3FfbPbuakEok1aaGMEg3O8ydaOiSsMjCcVWO8yB2w5jkVoEiJFmyvk5mQk2-qyy2UTRPHu0Wfx5TTI5nAOB4uWpZ6gg0W9lupKBgnp9OWW0JdRvjGV3mOnI_Lsl58jL0M7VcIeltdXDkaAEhbDKsHNX7cO8VZrhqA3mORl7XFIVWbW73WEdLhd3Yb9_Tf657KQaavFK1pCsnc-gjDzGeHc2R8yJfj7_28ZkeKNheL48XyVnGFOBMkiD-p9nRrfMcAaHa9E0UhX1Oe_CBG8L5Vhi50UFzeTDmOken5GGW6d4DtbmKkyHI0UcsFvHYIdmDwqkcFOifk7XMiV_QmJoIn87rvk5V5qKSngVkEoHpsut8356EORFa_0_wM-Cjed6irnGvmru168dFDrNeSdpsdwyFb4u35O-AgNfmm64slDlmJAxYPQciZKpxjKRnmA5CFjVWdRgwdoGgwXB2n--Zp1yamuJiiko8C-l9qbQUEPRhaUyEsWF8p_jG6dx8080J0hLNrsMhaooX2SGYGgKVKHlVy3kyldFtMIvUI5KOvb8Bz8MqLgEOHftRX1nMvKew_HRecXhs0vqtrljfSAqnceStU7CDl20adPnxlQJpwH09Gt8VXoETZ8orQp6xjhGI4zNRuq9D3l5-beQEHwddXiZUUxmnLs_QxTh5M9JVYPpAKX9mvSKKhm9poo2eGP8sJVjzv3FYFEEqN0hNuTpDJch-qoSZh0rfddpSZv8TXdsfPbUU-Spc7Lq0YclwMBoIo_F1H1EJqA-NLl9h5LVRAujhw3BH-b03L7CbPayuU4EChRiDe2NhWqGY_gPwtDKz0bhMZKMzdF5aPLB1yRuVxn7u05puIm_T7CR0BgdV_kU_47ZtXNMVgnjf99mz_tBZtXURZRx8y5W-8i5hg2IFm-3nrNPZTsXShHV0kMUvuJH4OafFd6mSXh6CJ8hDjdmpdQvDCMveekdCO9UKW3B3IVPyYJvRTugOfaXDaimOCu-MjN61nNJuldo9cedzn397Kw9rB_LHTnsK3nvAW_FnR2ZcbzBfk3O8b-3Ct73115nlzVZFMH_GIHKeVhqlHjaHqW3azcMOABK3syIMngcxB614z8QYyJXPNyv-A6xlbS68BdnuSHXFkRZIA_nbnV5GT-J7ge0Eem4hoWUlyYV2Jwuhh11J5ghxgFEcrBUkKXXmg42iykgspn-MgIVaA3xAl01dAz9yYqO920x74s6OaPwja99OnEzlQRusCFP_5Kj-h9xaAOqBWsfdF4eXUPyj9mq9kE27-KA30YRi6uIC_4uC-RH"
                    ?: throw IllegalStateException("DROPBOX_ACCESS_TOKEN environment variable is required"),
                appKey = System.getenv("DROPBOX_APP_KEY") ?:"w1qgodbesn97smf"
                    ?: throw IllegalStateException("DROPBOX_APP_KEY environment variable is required"),
                appSecret = System.getenv("DROPBOX_APP_SECRET")?:"ocfhohhzmadv90c"
                    ?: throw IllegalStateException("DROPBOX_APP_SECRET environment variable is required"),
                basePath = System.getenv("DROPBOX_BASE_PATH") ?: "/school-management"
            )
        }
    }
}