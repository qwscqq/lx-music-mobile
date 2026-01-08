package cn.toside.music.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactNativeHost;

public class MainActivity extends ReactActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity onCreate");
        
        // 注册ContentCatcher（关键修改！）
        registerContentCatcher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "MainActivity onResume");
        
        // 应用恢复时触发内容捕获
        triggerContentCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "MainActivity onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MainActivity onDestroy");
        
        // 注销ContentCatcher观察者
        unregisterContentCatcher();
    }

    @Override
    protected String getMainComponentName() {
        return "cn.toside.music.mobile";
    }

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new ReactActivityDelegate(this, getMainComponentName()) {
            @Override
            protected Bundle getLaunchOptions() {
                Bundle initialProperties = new Bundle();
                return initialProperties;
            }
        };
    }

    /**
     * 注册到MIUI ContentCatcher系统
     * 这是二次开发版实现CarWith/Jovincar歌词显示的关键
     */
    private void registerContentCatcher() {
        try {
            String packageName = getPackageName();
            String className = getClass().getName();
            
            Log.i(TAG, "尝试注册ContentCatcher: " + packageName + " - " + className);
            
            // 通过反射调用MIUI的ContentCatcherService
            Class<?> contentCatcherService = Class.forName("android.app.ContentCatcherService");
            java.lang.reflect.Method getInstanceMethod = contentCatcherService.getDeclaredMethod("getInstance");
            Object serviceInstance = getInstanceMethod.invoke(null);
            
            // 注册屏幕QA状态观察者
            java.lang.reflect.Method registerScreenQAMethod = contentCatcherService.getDeclaredMethod(
                "registerScreenQAStatusObserver", 
                String.class, 
                String.class
            );
            registerScreenQAMethod.invoke(serviceInstance, packageName, className);
            
            Log.i(TAG, "成功注册屏幕QA状态观察者");
            
            // 注册全局收集状态观察者
            java.lang.reflect.Method registerGlobalMethod = contentCatcherService.getDeclaredMethod(
                "registerGlobalCollectStatusObserver", 
                String.class, 
                String.class
            );
            registerGlobalMethod.invoke(serviceInstance, packageName, className);
            
            Log.i(TAG, "成功注册全局收集状态观察者");
            
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "ContentCatcherService不存在（非MIUI系统）");
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "ContentCatcher方法不存在（MIUI版本不同）");
        } catch (Exception e) {
            Log.e(TAG, "注册ContentCatcher失败", e);
        }
    }

    /**
     * 注销ContentCatcher观察者
     */
    private void unregisterContentCatcher() {
        try {
            String packageName = getPackageName();
            String className = getClass().getName();
            
            Class<?> contentCatcherService = Class.forName("android.app.ContentCatcherService");
            java.lang.reflect.Method getInstanceMethod = contentCatcherService.getDeclaredMethod("getInstance");
            Object serviceInstance = getInstanceMethod.invoke(null);
            
            // 注销屏幕QA状态观察者
            java.lang.reflect.Method unregisterScreenQAMethod = contentCatcherService.getDeclaredMethod(
                "unregisterScreenQAStatusObserver", 
                String.class, 
                String.class
            );
            unregisterScreenQAMethod.invoke(serviceInstance, packageName, className);
            
            // 注销全局收集状态观察者
            java.lang.reflect.Method unregisterGlobalMethod = contentCatcherService.getDeclaredMethod(
                "unregisterGlobalCollectStatusObserver", 
                String.class, 
                String.class
            );
            unregisterGlobalMethod.invoke(serviceInstance, packageName, className);
            
            Log.i(TAG, "已注销ContentCatcher观察者");
            
        } catch (Exception e) {
            Log.w(TAG, "注销ContentCatcher失败", e);
        }
    }

    /**
     * 触发内容捕获
     * 通知ContentCatcher当前应用有内容更新
     */
    private void triggerContentCapture() {
        try {
            // 通过反射调用ContentCatcher的内容更新方法
            Class<?> contentCatcherService = Class.forName("android.app.ContentCatcherService");
            java.lang.reflect.Method getInstanceMethod = contentCatcherService.getDeclaredMethod("getInstance");
            Object serviceInstance = getInstanceMethod.invoke(null);
            
            java.lang.reflect.Method triggerCaptureMethod = contentCatcherService.getDeclaredMethod(
                "triggerContentCapture",
                String.class,
                String.class
            );
            
            String packageName = getPackageName();
            String className = getClass().getName();
            triggerCaptureMethod.invoke(serviceInstance, packageName, className);
            
            Log.d(TAG, "已触发ContentCatcher内容捕获");
            
        } catch (Exception e) {
            Log.w(TAG, "触发ContentCatcher内容捕获失败", e);
        }
    }
}
