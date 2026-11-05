package cn.qwenwork.siriassistant;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 离线对话引擎 + 快捷指令（系统插件）：
 * 打开应用、查天气、定时、闹钟、打电话、搜索、拍照、系统设置等。
 * 对话界面不显示任何快捷指令入口，仅在输入框输入命令时触发。
 */
public class Assistant {

    public interface ActionRunner { void run(Intent intent); }

    public static class Reply {
        public final String text;
        public final Intent action; // 可为 null
        Reply(String text, Intent action) { this.text = text; this.action = action; }
    }

    private final Context ctx;
    private final Random random = new Random();

    public Assistant(Context ctx) { this.ctx = ctx; }

    /** 处理一条用户消息，返回回复（可能附带要执行的快捷指令 Intent）。 */
    public Reply respond(String raw) {
        String s = raw.trim();
        String low = s.toLowerCase(Locale.ROOT);

        // ---- 快捷指令帮助（输入「启动快捷指令」查看） ----
        if (containsAny(s, "启动快捷指令", "快捷指令", "打开快捷指令")) {
            return new Reply(
                "快捷指令已就绪。试试：\n" +
                "· 打开应用 微信\n" +
                "· 查天气\n" +
                "· 定时 10 分钟\n" +
                "· 闹钟 7点\n" +
                "· 搜索 今晚吃什么\n" +
                "· 打电话 10086\n" +
                "· 打开网址 baidu.com\n" +
                "· 拍照\n" +
                "· 打开设置 / 蓝牙 / WiFi", null);
        }

        // ---- 查天气 ----
        if (s.contains("天气")) {
            Intent it = findWeatherApp();
            if (it != null) return new Reply("好的，正在为你打开天气。", it);
            Intent web = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.baidu.com/s?wd=" + Uri.encode("今天天气")));
            return new Reply("没有找到天气应用，为你打开了天气搜索。", web);
        }

        // ---- 定时器 ----
        if (containsAny(s, "定时", "倒计时")) {
            long minutes = 1;
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)\\s*(个?小时|h|分钟|min|分|秒|s)").matcher(low);
            if (m.find()) {
                long v = Long.parseLong(m.group(1));
                String unit = m.group(2);
                if (unit.contains("小时") || unit.equals("h")) minutes = v * 60;
                else if (unit.contains("秒") || unit.equals("s")) minutes = Math.max(1, v / 60);
                else minutes = v;
            }
            Intent it = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                .putExtra(AlarmClock.EXTRA_MESSAGE, s)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            if (minutes <= 0) minutes = 1;
            return new Reply("好的，已设置 " + minutes + " 分钟的定时器。", it);
        }

        // ---- 闹钟 ----
        if (s.contains("闹钟")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\s*[点:：时]").matcher(low);
            Intent it = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                it.putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(AlarmClock.EXTRA_MINUTES, 0);
                return new Reply("好的，已为你设置 " + hour + " 点的闹钟。", it);
            }
            return new Reply("要设置几点的闹钟？例如：闹钟 7点", null);
        }

        // ---- 打电话 ----
        if (containsAny(s, "打电话", "拨打", "拨号")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{3,})").matcher(s);
            if (m.find()) {
                Intent it = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + m.group(1)));
                return new Reply("好的，正在为你拨号 " + m.group(1) + "。", it);
            }
            return new Reply("要拨打哪个号码？例如：打电话 10086", null);
        }

        // ---- 搜索 ----
        String query = afterAny(s, "搜索", "搜一下", "查询");
        if (query != null && !query.isEmpty()) {
            Intent it = new Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query);
            return new Reply("好的，正在为你搜索「" + query + "」。", it);
        }

        // ---- 打开网址 ----
        java.util.regex.Matcher um = java.util.regex.Pattern
            .compile("((https?://)?[\\w.-]+\\.[a-zA-Z]{2,}(\\S*))").matcher(s);
        String url;
        if (containsAny(s, "打开网址", "打开链接", "打开网站")) {
            url = um.find() ? um.group(1)
                 : afterAny(s, "打开网址", "打开链接", "打开网站");
            if (url == null || url.isEmpty())
                return new Reply("要打开哪个网址？例如：打开网址 baidu.com", null);
        } else if (um.find()) {
            url = um.group(1);
        } else {
            url = null;
        }
        if (url != null) {
            if (!url.startsWith("http")) url = "https://" + url;
            return new Reply("好的，正在为你打开网页。",
                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }

        // ---- 打开应用（须放在系统页面判断之后，避免“打开蓝牙”等被误拦截） ----
        String appName = afterAny(s, "打开应用", "启动应用", "打开", "启动", "运行");
        if (appName != null && !appName.isEmpty()) {
            Intent it = findApp(appName);
            if (it != null) return new Reply("好的，正在为你打开「" + appName + "」。", it);
            return new Reply("没有找到名为「" + appName + "」的应用，换个名字试试？", null);
        }

        // ---- 拍照 / 相机 ----
        if (containsAny(s, "拍照", "照相机", "打开相机")) {
            Intent it = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            return new Reply("好的，正在打开相机。", it);
        }

        // ---- 系统设置 ----
        if (containsAny(s, "蓝牙")) {
            return new Reply("好的，正在打开蓝牙设置。",
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        }
        if (containsAny(s, "wifi", "WiFi", "无线网")) {
            return new Reply("好的，正在打开 WLAN 设置。",
                new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        if (containsAny(s, "音量", "声音设置")) {
            return new Reply("好的，正在打开声音设置。",
                new Intent(Settings.ACTION_SOUND_SETTINGS));
        }
        if (containsAny(s, "打开设置", "系统设置")) {
            return new Reply("好的，正在打开系统设置。", new Intent(Settings.ACTION_SETTINGS));
        }

        // ---- 时间 / 日期 ----
        if (containsAny(s, "几点", "时间", "现在几点")) {
            return new Reply("现在是 " + new SimpleDateFormat("HH:mm", Locale.CHINA)
                .format(new Date()) + "。", null);
        }
        if (containsAny(s, "几号", "日期", "星期几", "今天是几")) {
            return new Reply("今天是 " + new SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA)
                .format(new Date()) + "。", null);
        }

        // ---- 闲聊 ----
        if (containsAny(s, "你好", "嗨", "hi", "hello", "哈喽", "在吗")) {
            return new Reply(pick("你好！有什么可以帮你的吗？",
                "嗨～我在这儿。想聊聊，还是让我帮你做点什么？",
                "你好呀！点我还能用语音说话哦。"), null);
        }
        if (containsAny(s, "你是谁", "你叫什么", "你是什么")) {
            return new Reply("我是 Siri，你的语音助手。点上面的光球就能用语音跟我说话。"
                + "输入「启动快捷指令」可以看到我能帮你做的事。", null);
        }
        if (containsAny(s, "谢谢", "感谢", "thanks")) {
            return new Reply(pick("不客气～", "随时为你效劳。", "小事一桩。"), null);
        }
        if (containsAny(s, "笑话", "讲个笑话", "逗我")) {
            return new Reply(pick(
                "为什么程序员不喜欢大自然？因为那里有太多 bug（虫子）。",
                "我问闹钟：你为什么这么吵？它说：不然怎么叫醒你呢？",
                "手机对充电器说：没有你，我活不过一天。"), null);
        }
        if (containsAny(s, "再见", "拜拜", "晚安")) {
            return new Reply(pick("再见～有事随时叫我。", "拜拜，祝你有美好的一天！"), null);
        }
        if (containsAny(s, "帮助", "你能做什么", "功能")) {
            return new Reply("我可以陪你聊天，也能帮你操作手机："
                + "打开应用、查天气、定时、闹钟、打电话、搜索、拍照、打开网页、打开系统设置……"
                + "输入「启动快捷指令」查看全部指令。", null);
        }
        if (low.contains("帅") || s.contains("漂亮")) {
            return new Reply("谢谢夸奖，你也很好看哦～", null);
        }

        // ---- 兜底 ----
        return new Reply(pick(
            "我在听～你可以问我时间，或者输入「启动快捷指令」让我帮你操作手机。",
            "这个问题有点难倒我了。试试让我打开应用、查天气或者定个时？",
            "嗯嗯，你说的我记下了。需要帮忙做点什么吗？输入「启动快捷指令」看看我的本领。"), null);
    }

    /** 尝试执行回复附带的快捷指令 Intent。返回是否成功。 */
    public boolean runAction(Intent intent) {
        if (intent == null) return true;
        try {
            ctx.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    // ---------- 工具 ----------

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) if (s.contains(k)) return true;
        return false;
    }

    /** 返回 s 中第一个关键词之后的内容；没有命中返回 null。 */
    private static String afterAny(String s, String... keys) {
        for (String k : keys) {
            int i = s.indexOf(k);
            if (i >= 0) {
                String rest = s.substring(i + k.length()).trim();
                if (!rest.isEmpty()) return rest;
            }
        }
        return null;
    }

    /** 按名称查找可启动的应用（模糊匹配标签）。 */
    private Intent findApp(String name) {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Intent best = null;
        for (ResolveInfo ri : apps) {
            String label = ri.loadLabel(pm).toString();
            if (label.equalsIgnoreCase(name) || label.contains(name) || name.contains(label)) {
                best = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (label.equalsIgnoreCase(name)) break;
            }
        }
        return best;
    }

    /** 查找天气应用。 */
    private Intent findWeatherApp() {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            String label = ri.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            String pkg = ri.activityInfo.packageName.toLowerCase(Locale.ROOT);
            if (label.contains("天气") || label.contains("weather")
                || pkg.contains("weather")) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
            }
        }
        return null;
    }
}
