package com.tencent.yolov8ncnn;

import static android.content.ContentValues.TAG;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingWindowService extends Service implements CardUpdateListener {
    private WindowManager windowManager;
    private ViewGroup floatView;
    private int LAYOUT_TYPE;
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private Context context = this;
    private int[] cardCounts;
    private int[] handCard = {200, 500, 3000, 1200};
    private int[] player1 = {2100, 320, 2750, 520};//左x，左y，右x，右y 左上角为0，0
    private int[] player3 = {330, 340, 900, 530};
    private int[] player2 = {1300, 200, 1800, 360};
    private int[] player0 = {1300, 420, 1800, 600};
    private int[] laizi = {290, 70, 370, 150};
    private float ratio; // 适配不同屏幕大小，需要缩放或扩大的比例
    private Yolov8Ncnn yolov8ncnn; // YOLOv8模型实例
    private int time;
    private GameRecorder gameRecorder;
    private boolean isTouchable=false;
    private int leftPadding = 0;
    private int topPadding = 0;
    private boolean isGameInProgress = false;
    private Handler autoStartHandler = new Handler();
    private Runnable autoStartRunnable;
    private boolean hasBegin = false;


    // 类成员变量
    private Player[] players;

    private List<HandCardOverlayView> overlayViews = new ArrayList<>();


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
        Display display = windowManager.getDefaultDisplay();

        // 获取屏幕密度
        mScreenDensity = metrics.densityDpi;
        Point size = new Point();
        display.getRealSize(size);
        mScreenWidth = size.x;
        mScreenHeight=size.y;
        // 计算比例因子，用于调整UI元素大小
        ratio = mScreenHeight / 3200f;
        for(int i=0;i<4;i++){
            handCard[i]=(int)(handCard[i]*ratio);
        }
        loadPlayerPositions();

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

        // 从SharedPreferences中读取时间配置，默认值为500毫秒
        SharedPreferences sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        time = sharedPreferences.getInt("time", 500);

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
        startButton.setEnabled(false);
        startButton.setText("禁用");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScreenshotEnabled) {
                    // 如果当前是“停止”状态，点击后停止截图和识别
                    stopScreenShot();
                    logGameDetails();
                    gameRecorder.saveGameRecord();
                    isGameInProgress = false;
                    setupAutoStartDetection();
                    startButton.setText("开始");
                } else {
                    // 如果当前是“开始”状态，点击后启动截图和识别
                    isGameInProgress = true;
                    autoStartHandler.removeCallbacks(autoStartRunnable);
                    createImageReader();
                    virtualDisplay();
                    initScreenShot();
                    startButton.setText("停止");
                }
                isScreenshotEnabled = !isScreenshotEnabled;
            }
        });

        // 设置停止按钮的点击事件
        Button setButton = floatView.findViewById(R.id.stopButton);
        setButton.setText("移位");

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isTouchable){
                    updatePlayerPositions();
                    setButton.setText("移位");
                    isTouchable=false;
                    setAllWindowsTouchable(isTouchable);
                }else{
                    setButton.setText("保存");
                    isTouchable=true;
                    setAllWindowsTouchable(isTouchable);
                }
            }
        });


    }
    private void calculateLeftPadding() {
        Display display = windowManager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 确保API级别支持
            DisplayCutout displayCutout = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                displayCutout = display.getCutout();
            }
            leftPadding = displayCutout != null ? displayCutout.getSafeInsetLeft() : 0;
            topPadding = displayCutout != null ? displayCutout.getSafeInsetTop() : 0;
        } else {
            leftPadding = 0; // 对于不支持 DisplayCutout 的设备，默认为 0
            topPadding = 0;
        }
        Log.d("left", String.valueOf(leftPadding));
        Log.d("top", String.valueOf(topPadding));
    }
    private void savePlayerPositions() {
        SharedPreferences prefs = getSharedPreferences("PlayerPositions", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 保存所有玩家区域坐标
        saveIntArray(editor, "player0", player0);
        saveIntArray(editor, "player1", player1);
        saveIntArray(editor, "player2", player2);
        saveIntArray(editor, "player3", player3);
        saveIntArray(editor, "laizi", laizi);

        editor.apply();
    }

    private void loadPlayerPositions() {
        SharedPreferences prefs = getSharedPreferences("PlayerPositions", MODE_PRIVATE);

        // 加载时使用默认值（原始比例计算值）
        player0 = loadIntArray(prefs, "player0", new int[]{1300, 420, 1800, 600});
        player1 = loadIntArray(prefs, "player1", new int[]{2100, 320, 2750, 520});
        player2 = loadIntArray(prefs, "player2", new int[]{1300, 200, 1800, 360});
        player3 = loadIntArray(prefs, "player3", new int[]{330, 340, 900, 530});
        laizi = loadIntArray(prefs, "laizi", new int[]{290, 70, 370, 150});
    }

    // 辅助方法：保存int数组
    private void saveIntArray(SharedPreferences.Editor editor, String key, int[] array) {
        for (int i = 0; i < array.length;  i++) {
            editor.putInt(key  + "_" + i, array[i]);
        }
    }

    // 辅助方法：加载int数组（带屏幕适配）
    private int[] loadIntArray(SharedPreferences prefs, String key, int[] defaultValues) {
        int[] result = new int[defaultValues.length];
        for (int i = 0; i < defaultValues.length;  i++) {
            // 默认值需要根据当前屏幕比例计算
            int defaultValue = (int)(defaultValues[i] * ratio);
            result[i] = prefs.getInt(key  + "_" + i, defaultValue);
        }
        return result;
    }
    private void updatePlayerPositions() {
        for (int i = 0; i < overlayViews.size(); i++) {
            HandCardOverlayView view = overlayViews.get(i);
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            int x = params.x + leftPadding; // 调整x坐标，加上leftPadding
            int y = params.y+ topPadding;

            int[] targetArray;
            if (i < 4) {
                switch (i) {
                    case 0:
                        targetArray = player0;
                        break;
                    case 1:
                        targetArray = player1;
                        break;
                    case 2:
                        targetArray = player2;
                        break;
                    case 3:
                        targetArray = player3;
                        break;
                    default:
                        continue; // 不可能的情况
                }
            } else {
                targetArray = laizi;
            }

            // 计算位置变化量
            int deltaX = x - targetArray[0];
            int deltaY = y - targetArray[1];

            // 更新数组中的坐标
            targetArray[0] = x; // 设置新的左上角x
            targetArray[1] = y; // 设置新的左上角y
            targetArray[2] += deltaX; // 调整右下角x
            targetArray[3] += deltaY; //
        }
        savePlayerPositions();
    }

    private void showHandCardOverlay() {
        List<int[]> initialCoordinates = getAdjustedPlayerCoordinates(); // 获取初始坐标数据
        for (int i = 0; i < initialCoordinates.size();  i++) {
            int[] rect = initialCoordinates.get(i);
            HandCardOverlayView view = new HandCardOverlayView(this, i, "Player " + (i));

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    rect[2] - rect[0]+30,
                    rect[3] - rect[1]+60,
                    getWindowType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);

            params.x = rect[0];
            params.y = rect[1];
            params.gravity  = Gravity.START | Gravity.TOP;

            windowManager.addView(view,  params);
            overlayViews.add(view);
        }
    }

    private int getWindowType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }


    public void setAllWindowsTouchable(boolean touchable) {
        for (HandCardOverlayView view : overlayViews) {
            updateWindowTouchFlag(view, touchable);
        }
    }

    private void updateWindowTouchFlag(View view, boolean touchable) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();

        // 使用位运算保持原有flags
        if (touchable) {
            // 移除不可触摸标志（按位非运算）
            params.flags  &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            // 保留原有flags并添加不可触摸标志（按位或运算）
            params.flags  |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }

        try {
            windowManager.updateViewLayout(view,  params);
        } catch (IllegalArgumentException e) {
            Log.e("OverlayService", "View not attached to window manager");
        }
    }

    private void removeHandCardOverlay() {
        // 双重校验保证线程安全
        if (windowManager == null || overlayViews == null) return;

        // 使用迭代器避免ConcurrentModificationException
        Iterator<HandCardOverlayView> iterator = overlayViews.iterator();
        while (iterator.hasNext())  {
            HandCardOverlayView view = iterator.next();
            try {
                // 在主线程执行移除操作
                new Handler(Looper.getMainLooper()).post(()  -> {
                    try {
                        if (view.isAttachedToWindow())  {
                            windowManager.removeView(view);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w("OverlayService", "View already removed: " + view);
                    }
                });
            } finally {
                iterator.remove();  // 确保从列表中移除引用
            }
        }

        // 强制垃圾回收
        System.gc();
    }
    private List<int[]> getAdjustedPlayerCoordinates() {
        List<int[]> adjustedPlayers = new ArrayList<>();

        for (int[] player : new int[][]{player0, player1, player2, player3, laizi}) {
            int[] adjustedPlayer = Arrays.copyOf(player, player.length); // 复制数组
            Log.d("left", String.valueOf(leftPadding));
            adjustedPlayer[0] -= leftPadding; // 左x坐标
            adjustedPlayer[2] -= leftPadding; // 右x坐标
            adjustedPlayers.add(adjustedPlayer);
        }

        return adjustedPlayers;
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

        // 获取开始按钮
        Button startButton = floatView.findViewById(R.id.startButton);

        // 根据屏幕方向更新按钮状态
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 如果是竖屏，禁用开始按钮并显示提示
            startButton.setEnabled(false);
            startButton.setText("禁用");
            stopScreenShot();
            removeHandCardOverlay();
        } else {
            // 如果是横屏，启用开始按钮
            startButton.setEnabled(true);
            startButton.setText("开始");
            calculateLeftPadding();
            showHandCardOverlay();
            createImageReader();
            virtualDisplay();
            setupAutoStartDetection();
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
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 5);
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
    private boolean isScreenshotEnabled = false;

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
        // 初始化玩家手牌计数数组，默认每种牌8张，王牌2张
        cardCounts = new int[16];
        Arrays.fill(cardCounts, 8);
        cardCounts[14] = 2;
        cardCounts[15] = 2;
        gameRecorder = new GameRecorder();
        players = new Player[]{
                new Player(player0, "Player 0"),
                new Player(player1, "Player 1"),
                new Player(player2, "Player 2"),
                new Player(player3, "Player 3")
        };
        for (Player player : players) {
            player.setCardUpdateListener(this);  // 设置Service为监听器
        }

        // 延迟1秒后执行一次截图操作
        handler.postDelayed(() -> {
            // 获取ImageReader中最新的图像
            Image image = mImageReader.acquireLatestImage();
            // 将图像转换为Bitmap格式
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            Bitmap bitmap4 = ImageUtils.cropBitmap(bitmap, laizi);
            // 裁剪出感兴趣的手牌区域
            bitmap = ImageUtils.cropBitmap(bitmap, handCard);
//            ImageUtils.saveBitmap(context, bitmap4);
            long startTime = System.currentTimeMillis();
            new OCRHelper().recognizeCharacter(bitmap4, new OCRHelper.OCRCallback() {
                @Override
                public void onResult(String character) {
                    runOnUiThread(() -> {
                        Log.d("OCR","识别结果: " + character);
                        GameRecorder.universalCard=character;
                        overlayViews.get(4).updateText(character);
                    });
                }
                @Override
                public void onError(String error) {
                    Log.e("OCR", "识别失败: " + error);
                    GameRecorder.universalCard="Q";
                    overlayViews.get(4).updateText("Q");
                }
            });
            long endTime=System.currentTimeMillis();
            Log.d("time", String.valueOf(endTime-startTime));
            int[] list = new int[100];
            Arrays.fill(list, 60);
            //识别手牌
            boolean success = yolov8ncnn.recognizeImage(bitmap, list, 1520); // 对玩家1的图片进行识别
            // 将YOLO识别的结果转换为playerCard格式
            int[] playerCard0 = CardUtils.convertYoloListToPlayerCard(list);
            if (27 - CardUtils.countCard(playerCard0) != 0) {
                Log.d("ErrorLog", "手牌识别错误");
                ImageUtils.saveBitmap(context, bitmap);
            }
            bitmap.recycle();
//            bitmap4.recycle();
            // 根据识别结果更新玩家手牌
            if (success && !CardUtils.isEmpty(playerCard0)) updateContent(playerCard0);
            // 开始连续截图
            startScreenShot();
        }, 1000);
    }

    // 在类中添加线程池
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // 修改startScreenShot方法
    private void startScreenShot() {
        if (isScreenshotEnabled) {
            handler.postDelayed(() -> executor.execute(() -> {
                long startTime = System.currentTimeMillis();
                Image image = mImageReader.acquireLatestImage();
                if (image == null) {
                    Log.e(TAG, "Image acquisition failed.");
                    return;
                }
                Bitmap bitmap = ImageUtils.imageToBitmap(image);
                image.close();

                for (Player player : players) {
                    if (player.count > 0) {

                        processPlayer(player, bitmap);
                    }
                }
                bitmap.recycle();
                long endTime=System.currentTimeMillis();
//                Log.d("time", String.valueOf(endTime-startTime));
                startScreenShot();
            }), time);
        }
    }

    private void processPlayer(Player player, Bitmap bitmap) {
        player.processPlayer(bitmap, yolov8ncnn, this);
    }

    // 实现回调接口方法
    @Override
    public void onCardsUpdated(String type,String s, int[] playedCards, int id) {
//        if (id != 0) {
//            updateContent(playedCards);
//        }
        overlayViews.get(id).updateText(s);
        handleGameRecorder(type,s, id);
    }

    private void handleGameRecorder(String type,String s, int actualPlayerId) {
        // 记录出牌
        if (!s.isEmpty()) {
            gameRecorder.recordPlay(actualPlayerId, type,s);
        }
        // 更新玩家状态
        if (players[actualPlayerId].count == 0) {
            gameRecorder.setPlayerFinished(actualPlayerId);
            Log.d("end", Integer.toString(actualPlayerId));
            if(gameRecorder.isGameEnded()){
                gameRecorder.saveGameRecord();
                hasBegin=true;
                Button startButton = floatView.findViewById(R.id.startButton);
                startButton.post(startButton::performClick);
            }
        }
    }

    private void logGameDetails() {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 4; i++) { // 假设有4个玩家
            // 获取玩家i的所有出牌记录
            String[] playerCards = gameRecorder.getPlayerCards(i);
            // 将出牌记录转换为一个字符串
            String cardsLog = String.join(", ", playerCards);
            log.append("玩家 ").append(i).append(" 出牌内容: ").append(cardsLog).append("\n");
        }
        // 输出到日志
        Log.d("GameDetails", log.toString());
        gameRecorder.generateLog();
    }

    // 定义牌面名称数组，包括特殊牌（小王、大王）
    String[] name = {" ", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "大王", "小王"};

    public void updateContent(int[] value) {
        runOnUiThread(() -> {
            for (int i = 1; i < 16; i++) {
                cardCounts[i] -= value[i];
                TextView textView = floatView.findViewById(
                        getResources().getIdentifier("textView" + i, "id", getPackageName()));
                textView.setText(name[i] + "\n" + cardCounts[i]);
            }
        });
    }

    private void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }

    private void stopScreenShot() {
        // 移除所有回调和消息，停止定时任务
        handler.removeCallbacksAndMessages(null);
    }

    private void setupAutoStartDetection() {
        autoStartRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameInProgress) {
                    checkHandCards();
                }
                autoStartHandler.postDelayed(this, 2000); // 每秒检测一次
            }
        };
        autoStartHandler.post(autoStartRunnable);
    }

    private void checkHandCards() {
        executor.execute(() -> {
            Image image = mImageReader.acquireLatestImage();
            Bitmap bitmap = ImageUtils.imageToBitmap(image);
            if(hasBegin){
                for (Player player : players) {
                    if (player.count > 0) {
                        gameRecorder.updateRemainingCards(Integer.parseInt(player.name),player.processFinalCards(bitmap, yolov8ncnn, this));
                        Log.d("remain:",player.name+player.processFinalCards(bitmap, yolov8ncnn, this));
                        runOnUiThread(() -> {
                            Toast.makeText(context, "玩家 " + player.name + " 剩余牌处理完成", Toast.LENGTH_SHORT).show();
                        });
                        gameRecorder.setPlayerFinished(Integer.parseInt(player.name));
                    }
                }
            }
            Bitmap handBitmap = ImageUtils.cropBitmap(bitmap, handCard);

            // 使用YOLO模型检测手牌数量
            int[] list = new int[100];
            Arrays.fill(list, 60);
            boolean success = yolov8ncnn.recognizeImage(handBitmap, list, 1520);

            if (success) {
                int cardCount = CardUtils.countCard(CardUtils.convertYoloListToPlayerCard(list));
                Log.d("121231",Integer.toString(cardCount));
                if (cardCount == 27) {
                    startNewGame();
                }
            }

            bitmap.recycle();
            handBitmap.recycle();
            image.close();
        });
    }

    private void startNewGame() {
        isGameInProgress = true;
        autoStartHandler.removeCallbacks(autoStartRunnable);
        // 启动屏幕截图和识别
        createImageReader();
        virtualDisplay();
        Button startButton = floatView.findViewById(R.id.startButton);
        startButton.post(startButton::performClick);

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

    // 在服务销毁时释放所有资源
    @Override
    public void onDestroy() {
        for (Player player : players) {
            player.setCardUpdateListener(null);
        }
        super.onDestroy();
        releaseResources();
        if (floatView != null && windowManager != null) {
            windowManager.removeView(floatView);
        }
    }

    private void releaseResources() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
}

