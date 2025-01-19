package com.tencent.yolov8ncnn;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private ViewGroup floatView;
    private int LAYOUT_TYPE;
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private Context context;
    private int[] cardCounts, player0Card,player1Card, player2Card,player3Card;
    private int[] handCard = {100, 500, 3000, 1200};
    private int[] player1 = {2300, 340, 2800, 530};//左x，左y，右x，右y 左上角为0，0
    private int[] player3 = {310, 320, 800, 520};
    private int[] player2 = {1200, 200, 2000, 360};
    private int[] player4 = {260, 340, 800, 554};
    private float ratio; // 适配不同屏幕大小，需要缩放或扩大的比例
    private Yolov8Ncnn yolov8ncnn; // YOLOv8模型实例

    private int time;

    @Override
    public void onCreate() {
        // 调用父类的onCreate方法
        super.onCreate();

        // 初始化YOLOv8模型实例
        yolov8ncnn = new Yolov8Ncnn();

        // 尝试加载模型文件
        if (!loadModel()) {
            Toast.makeText(this, "模型加载失败", Toast.LENGTH_SHORT).show();
        }


        // 获取应用的全局Context
        this.context = this.getApplicationContext();

        // 获取WindowManager服务
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 创建DisplayMetrics对象，用于获取屏幕尺寸和密度信息
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        // 获取屏幕密度
        mScreenDensity = metrics.densityDpi;

        // 获取屏幕宽度（像素）
        mScreenWidth = metrics.widthPixels;

        // 获取屏幕高度（考虑虚拟按键的高度）
        mScreenHeight = ImageUtils.getHasVirtualKey(windowManager);

        // 计算比例因子，用于调整UI元素大小
        ratio = mScreenHeight / 3200f;

        // 根据比例因子调整手牌和玩家相关数据
        for (int i = 0; i < handCard.length; i++) {
            handCard[i] = (int) (handCard[i] * ratio);
            player1[i] = (int) (player1[i] * ratio);
            player2[i] = (int) (player2[i] * ratio);
        }

        // 获取LayoutInflater服务，用于加载布局文件
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        // 加载浮动窗口的布局
        floatView = (ViewGroup) inflater.inflate(R.layout.floating_layout, null);

        // 判断系统版本以设置悬浮窗的类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_TOAST;
        }

        // 设置悬浮窗的参数
        floatWindowLayoutParam = new WindowManager.LayoutParams(mScreenWidth, mScreenHeight / 10, LAYOUT_TYPE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        floatWindowLayoutParam.gravity = Gravity.CENTER;
        floatWindowLayoutParam.x = 0;
        floatWindowLayoutParam.y = 0;

        // 从SharedPreferences中读取时间配置，默认值为2000毫秒
        SharedPreferences sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        time = sharedPreferences.getInt("time", 2000);

        // 将浮动视图添加到WindowManager
        windowManager.addView(floatView, floatWindowLayoutParam);

        // 设置触摸监听器以实现悬浮框的移动
        floatView.setOnTouchListener(new View.OnTouchListener() {
            final WindowManager.LayoutParams floatWindowLayoutUpdateParam = floatWindowLayoutParam;
            double x, y, px, py;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录按下时的位置和坐标
                        x = floatWindowLayoutUpdateParam.x;
                        y = floatWindowLayoutUpdateParam.y;
                        px = event.getRawX();
                        py = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 更新悬浮窗的位置
                        floatWindowLayoutUpdateParam.x = (int) ((x + event.getRawX()) - px);
                        floatWindowLayoutUpdateParam.y = (int) ((y + event.getRawY()) - py);
                        windowManager.updateViewLayout(floatView, floatWindowLayoutUpdateParam);
                        break;
                }
                return false;
            }
        });

        // 设置开始按钮的点击事件
        Button startButton = floatView.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createImageReader();
                virtualDisplay();
                initScreenShot();
            }
        });

        // 设置停止按钮的点击事件
        Button stopButton = floatView.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenShot();
            }
        });
    }

    // 尝试加载YOLOv8模型
    private boolean loadModel() {
        try {
            return yolov8ncnn.loadModel(getAssets(), 2, 0); // 使用默认的模型索引和CPU/GPU设置
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to load model: " + e.getMessage());
            return false;
        }
    }

    // 声明MediaProjectionManager和MediaProjection对象
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mMediaProjection;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // 从Intent中获取启动屏幕录制的结果码和数据
        int mResultCode = intent.getIntExtra("code", -1);
        Intent mResultData = intent.getParcelableExtra("data");

        // 获取MediaProjectionManager服务
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // 使用结果码和结果数据创建MediaProjection实例，用于进行屏幕录制
        mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, Objects.requireNonNull(mResultData));

        // 打印日志，表明MediaProjection已创建
        Log.e(TAG, "mMediaProjection created: " + mMediaProjection);

        // 返回默认的启动状态
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 调用父类的方法以确保系统正常处理配置变化
        super.onConfigurationChanged(newConfig);

        // 计算当前屏幕的最大值和最小值，以便在横竖屏切换时调整宽高
        int big = Math.max(mScreenHeight, mScreenWidth), small = Math.min(mScreenHeight, mScreenWidth);

        // 判断当前设备方向，并根据方向设置新的屏幕宽度和高度
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 如果是横屏，则宽度为较大值，高度为较小值
            mScreenWidth = big;
            mScreenHeight = small;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 如果是竖屏，则宽度为较小值，高度为较大值
            mScreenWidth = small;
            mScreenHeight = big;
        }

        // 获取WindowManager服务
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 检查windowManager是否成功获取
        if (windowManager != null) {
            // 更新浮动窗口布局参数中的宽度，并固定其高度为200像素
            floatWindowLayoutParam.width = mScreenWidth;
            floatWindowLayoutParam.height = 200;

            // 更新浮动视图的布局参数
            windowManager.updateViewLayout(floatView, floatWindowLayoutParam);
        }
    }

    // 定义屏幕宽度、高度及密度变量
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    // ImageReader实例，用于接收屏幕内容
    private ImageReader mImageReader;

    /**
     * 创建ImageReader实例，该实例用于接收屏幕镜像的数据流。
     * 如果mImageReader已经存在，则直接返回，避免重复创建。
     */
    private void createImageReader() {
        // 检查是否已存在ImageReader实例，如果存在则直接返回
        if (mImageReader != null) return;

        // 创建一个新的ImageReader实例，指定宽高、像素格式以及最大图像数量
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
    }

    // VirtualDisplay实例，用于显示或捕获屏幕内容
    private VirtualDisplay mVirtualDisplay;

    /**
     * 创建一个虚拟显示器，将屏幕内容输出到ImageReader中。
     * 如果mVirtualDisplay已经存在，则直接返回，避免重复创建。
     */
    private void virtualDisplay() {
        // 检查是否已存在VirtualDisplay实例，如果存在则直接返回
        if (mVirtualDisplay != null) return;

        // 使用MediaProjection创建一个虚拟显示器，将输出重定向到ImageReader的Surface
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    // 标识是否启用了截图功能，默认值为true
    private boolean isScreenshotEnabled = true;

    /**
     * 比较两个数组的内容，判断是否有任何变化。
     * 若current数组中的元素与last数组不一致，则返回true表示有变化；否则返回false。
     *
     * @param last 上一次的状态数组
     * @param current 当前状态数组
     * @return 是否发生变化
     */
    public boolean isChange(int[] last, int[] current) {
        // 首先检查两个数组是否为null或长度不一致
        if (last == null || current == null || last.length != current.length) {
            return true; // 如果任一数组为null或长度不一致，则认为有变化
        }

        // 比较last和current数组，若有不同之处，则返回true
        for (int i = 0; i < last.length; i++) {
            if (current[i] != last[i]) {
                return true;
            }
        }

        // 如果没有发现任何差异，则返回false
        return false;
    }

    // 初始化Handler实例，用于处理延迟任务或更新UI
    private Handler handler = new Handler();

    /**
     * 初始化屏幕截图过程。
     * 清除之前的所有回调消息，并设置按钮文本为"停止"。
     * 初始化玩家卡片计数，并启动定时任务进行截图。
     */
    public void initScreenShot() {
        // 移除所有之前的消息和回调，确保只运行最新的任务
        handler.removeCallbacksAndMessages(null);

        // 设置截图功能为启用状态
        isScreenshotEnabled = true;

        // 查找浮动视图中的"停止"按钮，并设置其文本
        TextView textView1 = floatView.findViewById(getResources().getIdentifier("stopButton", "id", getPackageName()));
        textView1.setText("停止");

        // 初始化玩家手牌计数数组，默认每种牌8张，王牌2张
        this.cardCounts = new int[16];
        this.player1Card = new int[16];
        this.player2Card = new int[16];
        Arrays.fill(cardCounts, 8);
        cardCounts[14] = 2;
        cardCounts[15] = 2;

        // 更新玩家手牌信息
        updatePlayerCard();

        // 延迟1秒后执行一次截图操作
        handler.postDelayed(new Runnable() {
            public void run() {
                // 获取ImageReader中最新的图像
                Image image = mImageReader.acquireLatestImage();

                // 将图像转换为Bitmap格式
                Bitmap bitmap = ImageUtils.imageToBitmap(image);

                // 裁剪出感兴趣的手牌区域
                bitmap = ImageUtils.cropBitmap(bitmap, handCard);
                int[] list= new int[100];
                Arrays.fill(list,60);
                boolean success = yolov8ncnn.recognizeImage(bitmap, list); // 对玩家1的图片进行识别

                saveBitmap(bitmap);


                // 将YOLO识别的结果转换为playerCard格式
                int[] playerCard0 = convertYoloListToPlayerCard(list);

                // 根据识别结果更新玩家手牌
                if (success && isChange(player0Card, playerCard0)) updateContent(playerCard0);

                // 更新玩家当前的手牌信息
                if (success) player0Card = list;
                // 开始连续截图
                startScreenShot();
            }
        }, 1000);
    }

    /**
     * 开始连续进行屏幕截图。
     * 如果启用了截图功能，则每隔一段时间自动进行截图并保存图片到本地存储。
     */
    private void startScreenShot() {
        if (isScreenshotEnabled) {
            // 定时执行截图操作
            handler.postDelayed(new Runnable() {
                public void run() {
                    // 获取最新图像
                    Image image = mImageReader.acquireLatestImage();
                    // 检查image是否为null
                    if (image == null) {
                        Log.e("FloatingWindowService", "Failed to acquire image, retrying...");
                        // 可以选择立即重试或等待一段时间后重试
                        startScreenShot();
                        return;
                    }
                    // 转换为Bitmap格式
                    Bitmap bitmap = ImageUtils.imageToBitmap(image);
                    // 分别裁剪出两个玩家的手牌区域
                    Bitmap bitmap1 = ImageUtils.cropBitmap(bitmap, player1);
                    Bitmap bitmap2 = ImageUtils.cropBitmap(bitmap, player2);
                    Bitmap bitmap3 = ImageUtils.cropBitmap(bitmap, player3);
//                    Bitmap bitmap4 = ImageUtils.cropBitmap(bitmap, player4);
                    // 初始化用于存储识别结果的数组
                    int[] list1 = new int[100]; // 假设最多有100个标签
                    int[] list2 = new int[100];
                    int[] list3 = new int[100];
                    Arrays.fill(list1,60);
                    Arrays.fill(list2,60);
                    Arrays.fill(list3,60);
                    // 使用YOLO模型进行识别
                    boolean success1 = yolov8ncnn.recognizeImage(bitmap1, list1); // 对玩家1的图片进行识别
                    boolean success2 = yolov8ncnn.recognizeImage(bitmap2, list2); // 对玩家2的图片进行识别
                    boolean success3 = yolov8ncnn.recognizeImage(bitmap3, list3); // 对玩家2的图片进行识别
                    // 将YOLO识别的结果转换为playerCard格式
                    int[] playerCard1 = convertYoloListToPlayerCard(list1);
                    int[] playerCard2 = convertYoloListToPlayerCard(list2);
                    int[] playerCard3 = convertYoloListToPlayerCard(list3);

                    // 根据识别结果更新玩家手牌
                    if (success1 && isChange(player1Card, playerCard1)){
                        Log.d("GameLog", "player1出牌");
                        player1Card = playerCard1;
                        logPlayerCard(player1Card);
                        updateContent(player1Card);
                        if(!isEmpty(playerCard1)){
                            saveBitmap(bitmap1);
                        }
                    }
                    if (success2 && isChange(player2Card, playerCard2)){
                        Log.d("GameLog", "player2出牌");
                        player2Card = playerCard2;
                        logPlayerCard(player2Card);
                        updateContent(player2Card);
                        if(!isEmpty(playerCard2)){
                            saveBitmap(bitmap2);
                        }
                    }
                    if (success3 && isChange(player3Card, playerCard3)){
                        Log.d("GameLog", "player3出牌");
                        player3Card = playerCard3;
                        logPlayerCard(player3Card);
                        updateContent(player3Card);
                        if(!isEmpty(playerCard3)){
                            saveBitmap(bitmap3);
                        }
                    }
                    // 更新玩家手牌信息
                    updatePlayerCard();
                    // 继续下一轮截图
                    startScreenShot();
                }
            }, time);
        }
    }
    private void logPlayerCard(int[] playerCard) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < playerCard.length; i++) {
            if (playerCard[i] != 0) {
                for (int j = 0; j < playerCard[i]; j++) {
                    if (i == 14) {
                        sb.append("小王");
                    } else if (i == 15) {
                        sb.append("大王");
                    } else {
                        sb.append(i);
                    }
                    sb.append(" ");
                }
            }
        }
        Log.d("GameLog", sb.toString());
    }
    private boolean isEmpty(int[] playerCard) {
        for (int i = 1; i < playerCard.length; i++) {
            if (playerCard[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private void saveBitmap(Bitmap bitmap) {
        // 定义保存Bitmap的文件路径和文件名
        String filename = UUID.randomUUID().toString() + ".png";
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appPicturesDirectory = new File(picturesDirectory, "MyAppFolder"); // "MyAppFolder"是你应用的文件夹
        if (!appPicturesDirectory.exists()) {
            if (!appPicturesDirectory.mkdirs()) {
                Log.e("BitmapSave", "Failed to create directory for saving images.");
                return;
            }
        }
        File outputFile = new File(appPicturesDirectory, filename);

        try {
            // 将Bitmap压缩成文件
            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // 100表示不压缩
            out.flush();
            out.close();

            // 将文件插入到媒体库，这样相册应用就能看到这张图片了
            MediaScannerConnection.scanFile(this, new String[]{outputFile.getAbsolutePath()}, null, null);

            // 提示用户图片已保存
            Toast.makeText(this, "Image saved: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("BitmapSave", "Error saving bitmap: " + e.getMessage());
        }
    }

    // 将YOLO识别结果转换为playerCard格式
    private int[] convertYoloListToPlayerCard(int[] yoloList) {
        int[] playerCard = new int[16]; // 初始化长度为15的数组

        for (int id : yoloList) {
            if (id >= 0 && id < 52) { // 确保ID在有效范围内
                int cardValue = id / 4+1; // 计算牌面值，忽略花色
                if (cardValue > 0 && cardValue <= 13) { // A到K的范围
                    playerCard[cardValue]++; // 对应位置计数加一
                }
            } else if (id == 52) { // 小王
                playerCard[14]++;
            } else if (id == 53) { // 大王
                playerCard[15]++;
            }
        }
        return playerCard;
    }
    // 定义牌面名称数组，包括特殊牌（小王、大王）
    String[] name = { " ","A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "小王", "大王"};

    /**
     * 更新显示内容。
     * 根据给定的价值数组更新卡片计数，并刷新UI上的文本视图以反映新的计数。
     */
    public void updateContent(int[] value) {
        Log.d("FloatingWindowService", "Values: " + Arrays.toString(value));
        for (int i = 1; i < 16; i++) {
            // 减去相应的卡片数量
            cardCounts[i] -= value[i];
            // 查找并更新对应的TextView
            TextView textView = floatView.findViewById(getResources().getIdentifier("textView" + i, "id", getPackageName()));
            // 设置文本为当前牌面名及其剩余数量
            textView.setText(name[i] + "\n" + cardCounts[i]);
        }
    }

    /**
     * 更新玩家手牌信息。
     * 根据player1Card和player2Card数组中的数据构建两个玩家的手牌表示，并更新UI。
     */
    public void updatePlayerCard() {
        // 使用StringBuilder来构建玩家手牌字符串
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();

        // 遍历所有牌面值
        for (int i = 1; i < 15; i++) {
            // 如果玩家1拥有该牌，则将其名字添加到s1中相应次数
            if (player1Card[i] != 0) {
                for (int j = 0; j < player1Card[i]; j++) {
                    s1.append(name[i]);
                }
            }
            // 对于玩家2执行相同操作
            if (player2Card[i] != 0) {
                for (int j = 0; j < player2Card[i]; j++) {
                    s2.append(name[i]);
                }
            }
        }

        // 如果玩家手牌为空，则设置默认文本为“空”
        if (s1.length() == 0) s1.append("空");
        if (s2.length() == 0) s2.append("空");

        // 更新UI显示
//        TextView textView1 = floatView.findViewById(getResources().getIdentifier("player1", "id", getPackageName()));
//        textView1.setText(s1.toString());
//        TextView textView2 = floatView.findViewById(getResources().getIdentifier("player2", "id", getPackageName()));
//        textView2.setText(s2.toString());
//
//        // 显示当前时间（秒级），用于调试或参考
//        TextView textView = floatView.findViewById(getResources().getIdentifier("textView", "id", getPackageName()));
//        textView.setText("时间" + (System.currentTimeMillis() / 1000) % 100);
    }

    /**
     * 停止或继续截图。
     * 切换isScreenshotEnabled标志位，并根据状态更新按钮文本。如果重新启用截图，则调用startScreenShot()方法开始截图流程。
     */
    private void stopScreenShot() {
        if (isScreenshotEnabled) {
            // 禁用截图功能
            isScreenshotEnabled = false;
            // 移除所有回调和消息，停止定时任务
            handler.removeCallbacksAndMessages(null);
            // 更新按钮文本为“继续”
            TextView textView1 = floatView.findViewById(getResources().getIdentifier("stopButton", "id", getPackageName()));
            textView1.setText("继续");
        } else {
            // 启用截图功能
            isScreenshotEnabled = true;
            // 更新按钮文本为“停止”
            TextView textView1 = floatView.findViewById(getResources().getIdentifier("stopButton", "id", getPackageName()));
            textView1.setText("停止");
            // 开始截图过程
            startScreenShot();
        }
    }

    /**
     * 绑定服务时调用的方法。
     * 此实现返回null，意味着该服务不支持绑定。
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}