package com.tencent.yolov8ncnn;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private EditText usernameEditText;
    private EditText passwordEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        usernameEditText = view.findViewById(R.id.username);
        passwordEditText = view.findViewById(R.id.password);
        Button loginButton = view.findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                User user = new User(username, password, (byte) 0);
//                sendLoginRequest(user);
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_mainFragment);
            }
        });

        return view;
    }

    private void sendLoginRequest(User user) {
        if (!isAdded()) return; // 确保Fragment已附加

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<Map<String, Object>> call = apiService.login(user);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!isAdded()) return; // 再次检查状态

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    Object status = result.get("status");
                    assert status != null;
                    int code = ((Number) status).intValue();

                    if (code==200) {
                        Map<String, String> data = (Map<String, String>) result.get("data");
                        if (data != null) {
                            String token = data.get("token");
                            if (token != null) {
                                saveToken(token);
                                showToast("登录成功");
                                Log.d("LoginFragment", "Token: " + token);
                                navigateToMain();
                                Log.d("LoginFragment", "Token: " + token);
                            } else {
                                showToast("Token缺失");
                            }
                        } else {
                            showToast("数据为空");
                        }
                    } else {
                        String message = (String) result.get("message");
                        showToast(message != null ? message : "登录失败");
                    }
                } else {
                    showToast("请求失败: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (isAdded()) showToast("网络错误: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToMain() {
        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_mainFragment);
        }
    }

    private void saveToken(String token) {
        // 示例：保存 token 到 SharedPreferences
        android.content.Context context = getContext();
        if (context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("token", token).apply();
        }
    }
}