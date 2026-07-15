import { reactive } from "vue";
import type { FormRules } from "element-plus";
import { $t, transformI18n } from "@/plugins/i18n";

/** 登录校验 */
const loginRules = reactive<FormRules>({
  username: [
    {
      required: true,
      message: transformI18n($t("login.pureUsernameReg")),
      trigger: "blur"
    },
    {
      max: 64,
      message: transformI18n($t("login.pureUsernameLength")),
      trigger: "blur"
    }
  ],
  password: [
    {
      required: true,
      message: transformI18n($t("login.purePassWordReg")),
      trigger: "blur"
    },
    {
      max: 128,
      message: transformI18n($t("login.purePassWordLength")),
      trigger: "blur"
    }
  ]
});

export { loginRules };
