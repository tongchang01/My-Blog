import { ElMessageBox } from "element-plus";

export async function showDemoLoginNotice(
  translate: (key: string) => string
): Promise<void> {
  await ElMessageBox.alert(
    translate("login.demoNotice.message"),
    translate("login.demoNotice.title"),
    {
      type: "warning",
      confirmButtonText: translate("login.demoNotice.confirm"),
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    }
  );
}
