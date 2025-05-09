package com.tencent.yolov8ncnn;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查是否是从悬浮窗跳转过来的
        boolean fromFloatingWindow = getIntent().getBooleanExtra("FROM_FLOATING_WINDOW", false);

        // 设置导航宿主
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // 如果是从悬浮窗跳转过来的，直接导航到 MainFragment
        if (fromFloatingWindow) {
            navController.navigate(R.id.mainFragment);
        }

        // 设置导航栏（可选）
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder()
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }
}