package com.qihoo360.replugin.sample.host;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class DynamicLoader {
    private static final String TAG = "DynamicLoader";
    private final DexClassLoader mClassLoader;
    public DynamicLoader(String apkPath, Context context) {
        String cache = apkPath + "cache"; //context.getCacheDir().getPath()
        Log.i(TAG, "DynamicLoader apkPath=" + apkPath + " cache=" + cache);
        mClassLoader = new DexClassLoader(
                apkPath, // APK路径
                cache, // 优化后的dex输出目录
                null, // 库路径
                context.getClassLoader() // 父类加载器
        );
    }

    public DexClassLoader getClassLoader() {
        return mClassLoader;
    }
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        //Class<?> pluginClass = classLoader.loadClass("com.example.PluginActivity");
        return mClassLoader.loadClass(className);
    }

    public Resources LoadResources(String apkPath, Context context) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        AssetManager assetManager = AssetManager.class.newInstance();
        Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
         addAssetPath.invoke(assetManager, apkPath);
        return new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }
}
