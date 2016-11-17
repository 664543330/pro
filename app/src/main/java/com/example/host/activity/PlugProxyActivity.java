package com.example.host.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import dalvik.system.DexClassLoader;

/**
 * Created by wangcong on 2016/11/17.
 */
public class PlugProxyActivity extends BaseRes {

    private Object pluginActivity;
    private Class<?> pluginClass;

    private HashMap<String, Method> methodMap = new HashMap<String,Method>();

    private SharedPreferences sharedPreferences;

    private static PCallback callback;

    private final int FROM_EXTERNAL = 0;

    private final String FROM = "extra.from";

    private final String VIEW_ACTION="com.example.dynamic.activity.Intent";

    public static final String KEY_APK = "path.apk";
    public static final String KEY_CLASS = "name.class";
    public static final String KEY_SCAN_MSG = "scanMsg";

    private String className;
    private String apkPath;

    private String dexOutputPath;

    private final String TAG="PlugProxy";


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apkPath =getIntent().getStringExtra(KEY_APK);
        className=getIntent().getStringExtra(KEY_CLASS);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            DexClassLoader loader = initClassLoader();

            pluginClass = loader.loadClass(className);
            Constructor<?> localConstructor = pluginClass.getConstructor(new Class[] {});
            pluginActivity = localConstructor.newInstance(new Object[] {});

            transmitMsg();

            Bundle bundle = new Bundle();
            bundle.putInt(FROM, FROM_EXTERNAL);
            executeMethod("onCreate",Bundle.class,bundle );

        } catch (Exception e) {
            Log.i(TAG, "load activity error:"+Log.getStackTraceString(e));
        }
    }

    public static void scan(PCallback pCallback){
        callback=pCallback;
    }

    private void transmitMsg(){
        executeMethod("setProxy",Activity.class,this );
        executeMethod("setDexPath",String.class,dexOutputPath );
        executeMethod("setProxyViewAction",String.class,VIEW_ACTION );
        executeMethod("setKeyScanMsg",String.class,KEY_SCAN_MSG );
    }

    @SuppressLint("NewApi")
    private DexClassLoader initClassLoader(){
        File dexOutputDir = this.getDir("dex", 0);
        dexOutputPath = dexOutputDir.getAbsolutePath();
        loadResources(apkPath);
        DexClassLoader loader = new DexClassLoader(apkPath, dexOutputPath,null , getClass().getClassLoader());
        return loader;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "proxy onDestroy");
        executeMethod("onDestroy");
        callback.scan(sharedPreferences.getString("scan",null));
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(TAG, "proxy onPause");
        executeMethod("onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "proxy onResume");
        executeMethod("onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "proxy onStart");
        executeMethod("onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "proxy onStop");
        executeMethod("onStop");
    }

    /**
     * 无formalParameter
     * @param methodName
     */
    private void executeMethod(String methodName){
        executeMethod(methodName,new Class[]{},new Object[]{});
    }


    /**
     * 单formalParameter
     * @param methodName
     * @param formalParameter
     * @param actualParameter
     */
    private void executeMethod(String methodName,Class formalParameter,Object actualParameter){
        executeMethod(methodName,new Class[]{formalParameter},new Object[]{actualParameter});
    }

    /**
     * 多formalParameter
     * @param methodName
     * @param formalParameter
     * @param actualParameter
     */
    private void executeMethod(String methodName,Class[] formalParameter,Object[] actualParameter){
        Method method = null;
        try {
            method = pluginClass.getMethod(methodName,formalParameter);
            method.setAccessible(true);
            method.invoke(pluginActivity, actualParameter);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "NoSuchMethodException:"+Log.getStackTraceString(e));
        } catch (InvocationTargetException e) {
            Log.d(TAG, "InvocationTargetException:"+Log.getStackTraceString(e));
        } catch (IllegalAccessException e) {
            Log.d(TAG, "IllegalAccessException:"+Log.getStackTraceString(e));
        }

    }


}
