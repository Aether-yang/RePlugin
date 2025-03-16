/*
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.qihoo360.replugin.sample.host;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.model.PluginInfo;
import com.qihoo360.replugin.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import xyz.xxin.fileselector.FileSelector;
import xyz.xxin.fileselector.beans.FileBean;
import xyz.xxin.fileselector.interfaces.OnResultCallbackListener;

/**
 * @author RePlugin Team
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_start_demo1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 刻意以“包名”来打开
                RePlugin.startActivity(MainActivity.this, RePlugin.createIntent("com.qihoo360.replugin.sample.demo1", "com.qihoo360.replugin.sample.demo1.MainActivity"));
            }
        });

        findViewById(R.id.btn_start_plugin_for_result).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 刻意以“Alias（别名）”来打开
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("demo1", "com.qihoo360.replugin.sample.demo1.activity.for_result.ForResultActivity"));
                RePlugin.startActivityForResult(MainActivity.this, intent, REQUEST_CODE_DEMO1, null);
            }
        });

        findViewById(R.id.btn_load_fragment_from_demo1).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PluginFragmentActivity.class));
            }
        });

        findViewById(R.id.btn_start_demo3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 若没有安装，则直接提示“错误”
                // TODO 将来把回调串联上
                if (RePlugin.isPluginInstalled("demo3")) {
                    RePlugin.startActivity(MainActivity.this, RePlugin.createIntent("demo3", "com.qihoo360.replugin.sample.demo3.MainActivity"));
                } else {
                    Toast.makeText(MainActivity.this, "You must install demo3 first!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_start_demo4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 示例：直接通过宿主打开WebView插件中的Activity
                // FIXME: 后续可以将webview MainActivity URL 改为动态传入
                // 若没有安装，则直接提示“错误”
                if (RePlugin.isPluginInstalled("webview")) {
                    RePlugin.startActivity(MainActivity.this, RePlugin.createIntent("webview", "com.qihoo360.replugin.sample.webview.MainActivity"));
                } else {
                    Toast.makeText(MainActivity.this, "You must install webview first!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_install_apk_from_assets).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final ProgressDialog pd = ProgressDialog.show(MainActivity.this, "Installing...", "Please wait...", true, true);
                // FIXME: 仅用于安装流程演示 2017/7/24
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        simulateInstallExternalPlugin();
                        pd.dismiss();
                    }
                }, 1000);
            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch swtApkHookTypeDynaLoader = findViewById(R.id.swt_apk_hook_type);
        swtApkHookTypeDynaLoader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "swt_apk_hook_type with mIsApkHookTypeRePlugin=" + mIsApkHookTypeRePlugin);
                mIsApkHookTypeRePlugin = !swtApkHookTypeDynaLoader.isChecked();
                //Toast.makeText(MainActivity.this, "swt_apk_hook_type chosen plugin", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_uninstall_apk_from_chosen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean bret = uninstallChosenPlugin();
                        Log.i(TAG, "uninstall chosen plugin bret=" + bret);
                        //Toast.makeText(MainActivity.this, "uninstall chosen plugin", Toast.LENGTH_SHORT).show();
                    }
                }, 1000);
            }
        });
        findViewById(R.id.btn_install_apk_from_chosen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileSelector.create(MainActivity.this)
                    .setInitPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())   // 设置起始路径
                    .isOnlySelectFile(true) // 只能选择文件
                    //.addIconBySuffix("apk", R.drawable.fs_file)  // 给指定类型文件添加图标
                    .addDisplayType("apk", "rar")    // 仅显示指定类型文件
                    .isOnlySelectFile(true) // 只能选择文件
                    .setMaxSelectValue(1)    // 设置文件最大选择数
                    .isAutoOpenSelectMode(true) // 自动打开选择模式
                    .isSingle(true)     // 设置为单选模式
                    .isVibrator(true)   // 开启震动
                    .forResult(new OnResultCallbackListener() {
                        @Override
                        public void onResult(List<FileBean> result) {
                            // 文件处理逻辑
                            String apkPath = result.get(0).getFile().getAbsolutePath();
                            Toast.makeText(MainActivity.this, "Chosen apk path="+apkPath, Toast.LENGTH_SHORT).show();
                            if(!mIsApkHookTypeRePlugin) {
                                mApkPath = apkPath;
                                return;
                            }
                            final ProgressDialog pd = ProgressDialog.show(MainActivity.this, "Installing...", "Please wait...", true, true);
                            // FIXME: 仅用于安装流程演示 2017/7/24
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //simulateInstallExternalPlugin();
                                    boolean bret = installChosenPluginApk(apkPath);
                                    Log.e(TAG, "installChosenPluginApk bret=" + bret);
                                    Toast.makeText(MainActivity.this, "installChosenPluginApk bret=" + bret, Toast.LENGTH_SHORT).show();
                                    pd.dismiss();
                                }
                            }, 1000);
                        }

                        @Override
                        public void onCancel() {
                            // 未选择处理逻辑
                            Log.e(TAG, "No apk chosen");
                            Toast.makeText(MainActivity.this, "No apk chosen", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        });

        ToggleButton btnInvoke = findViewById(R.id.btn_invoke_apk_method);
        btnInvoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "btnInvoke isChecked=" + btnInvoke.isChecked());
                boolean bret = invokePluginMethod(mIsApkHookTypeRePlugin, btnInvoke.isChecked());
                Log.i(TAG, "btnInvoke bret=" + bret);
                if(!bret) {
                    Toast.makeText(MainActivity.this, "btnInvoke bret=" + bret, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 刻意使用Thread的ClassLoader来测试效果
        testThreadClassLoader();
    }

    boolean mIsApkHookTypeRePlugin = true;
    String mApkPath = null;

    private void testThreadClassLoader() {
        // 在2.1.7及以前版本，如果直接调用此方法，则拿到的ClassLoader可能是PathClassLoader或者为空。有极个别Java库会用到此方法
        // 这里务必确保：cl == getClassLoader()，才符合预期
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != getClassLoader()) {
            throw new RuntimeException("Thread.current.classLoader != getClassLoader(). cl=" + cl + "; getC=" + getClassLoader());
        }
    }

    private static final int REQUEST_CODE_DEMO1 = 0x011;
    private static final int RESULT_CODE_DEMO1 = 0x012;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_DEMO1 && resultCode == RESULT_CODE_DEMO1) {
            Toast.makeText(this, data.getStringExtra("data"), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 模拟安装或升级（覆盖安装）外置插件
     * 注意：为方便演示，外置插件临时放置到Host的assets/external目录下，具体说明见README</p>
     */
    private void simulateInstallExternalPlugin() {
        String demo3Apk= "demo3.apk";
        String demo3apkPath = "external" + File.separator + demo3Apk;

        // 文件是否已经存在？直接删除重来
        String pluginFilePath = getFilesDir().getAbsolutePath() + File.separator + demo3Apk;
        File pluginFile = new File(pluginFilePath);
        if (pluginFile.exists()) {
            FileUtils.deleteQuietly(pluginFile);
        }

        // 开始复制
        copyAssetsFileToAppFiles(demo3apkPath, demo3Apk);
        PluginInfo info = null;
        if (pluginFile.exists()) {
            info = RePlugin.install(pluginFilePath);
        }

        if (info != null) {
            RePlugin.startActivity(MainActivity.this, RePlugin.createIntent(info.getName(), "com.qihoo360.replugin.sample.demo3.MainActivity"));
        } else {
            Toast.makeText(MainActivity.this, "install external plugin failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从assets目录中复制某文件内容
     *  @param  assetFileName assets目录下的Apk源文件路径
     *  @param  newFileName 复制到/data/data/package_name/files/目录下文件名
     */
    private void copyAssetsFileToAppFiles(String assetFileName, String newFileName) {
        InputStream is = null;
        FileOutputStream fos = null;
        int buffsize = 1024;

        try {
            is = this.getAssets().open(assetFileName);
            fos = this.openFileOutput(newFileName, Context.MODE_PRIVATE);
            int byteCount = 0;
            byte[] buffer = new byte[buffsize];
            while((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String mPluginName = "demo3";

    private boolean uninstallChosenPlugin() {
        boolean bret = false;
        Log.i(TAG, "uninstallChosenPlugin mPluginName=" + mPluginName);
        if(mPluginName != null) {
            bret = RePlugin.uninstall(mPluginName);
        }
        Toast.makeText(MainActivity.this, "uninstall plugin "+ mPluginName
                        + " bret=" + bret + " Run=" + RePlugin.isPluginRunning(mPluginName)
                        + " Inst=" + RePlugin.isPluginInstalled(mPluginName)+ " Used=" + RePlugin.isPluginUsed(mPluginName),
                Toast.LENGTH_LONG).show();
        //mPluginName = null;
        return bret;
    }

    private boolean installChosenPluginApk(String pluginFilePath) {
        // 文件是否已经存在？直接删除重来
        File pluginFile = new File(pluginFilePath);
        //FileUtils.deleteQuietly(pluginFile);
        PluginInfo info = null;
        if (pluginFile.exists()) {
            //RePlugin.getConfig().setMoveFileWhenInstalling(false);
            info = RePlugin.install(pluginFilePath);
        }
        Log.i(TAG, "install file=" + pluginFilePath + " " + pluginFile.exists() + " info=" + info);

        if (info == null) {
            Toast.makeText(MainActivity.this, "install external plugin failed", Toast.LENGTH_SHORT).show();
            return false;
        }
        //RePlugin.startActivity(MainActivity.this, RePlugin.createIntent(info.getName(), "com.qihoo360.replugin.sample.demo3.MainActivity"));
        //String pluginName = "com.qihoo360.replugin.sample.demo1";
        mPluginName = info.getName();
        return RePlugin.preload(mPluginName);
        //return invokeMethod(mPluginName);
    }
    private boolean invokePluginMethod(boolean isRePlugin, boolean isOn) {
        Log.w(TAG, "invokePluginMethod isOn=" + isOn);
        if(isRePlugin) {
            ClassLoader loader = loadClassFromPlugin(mPluginName);
            return invokeMethod(loader);
        }
        DynamicLoader dynaLoader = new DynamicLoader(mApkPath, MainActivity.this);
        return invokeMethod(dynaLoader.getClassLoader());
    }

    private ClassLoader loadClassFromPlugin(String pluginName) {
        Log.w(TAG, "invokeMethod pluginName=" + pluginName);
        if (pluginName == null) {
            return null;
        }
        /**
         * 注意：
         *
         * 如果一个插件是内置插件，那么这个插件的名字就是文件的前缀，比如：demo1.jar插件的名字就是demo1(host-gradle插件自动生成)，可以执行诸如RePlugin.fetchClassLoader("demo1")的操作；
         * 如果一个插件是外置插件，通过RePlugin.install("/sdcard/demo1.apk")安装的，则必须动态获取这个插件的名字来使用：
         * PluginInfo pluginInfo = RePlugin.install("/sdcard/demo1.apk");
         * RePlugin.preload(pluginInfo);//耗时
         * String name = pluginInfo != null ? pluginInfo.getName() : null;
         * ClassLoader classLoader = RePlugin.fetchClassLoader(name);
         */
        //注册相关Fragment的类
        //注册一个全局Hook用于拦截系统对XX类的寻找定向到Demo1中的XX类主要是用于在xml中可以直接使用插件中的类
        //RePlugin.registerHookingClass("com.qihoo360.replugin.sample.demo1.fragment.DemoFragment", RePlugin.createComponentName(pluginName, "com.qihoo360.replugin.sample.demo1.fragment.DemoFragment"), null);
        //setContentView(R.layout.activity_plugin_fragment);

        //代码使用插件Fragment
        ClassLoader d1ClassLoader = RePlugin.fetchClassLoader(pluginName);//获取插件的ClassLoader
        return d1ClassLoader;
    }
    private boolean invokeMethod(ClassLoader d1ClassLoader) {
        Log.i(TAG, "invokeMethod d1ClassLoader=" + d1ClassLoader);
        if (d1ClassLoader == null) {
            Log.e(TAG, "invokeMethod d1ClassLoader=" + d1ClassLoader);
            return false;
        }
        try {
            //Fragment fragment = d1ClassLoader.loadClass("com.qihoo360.replugin.sample.demo1.fragment.DemoCodeFragment").asSubclass(Fragment.class).newInstance();//使用插件的Classloader获取指定Fragment实例
            //getSupportFragmentManager().beginTransaction().add(R.id.container2, fragment).commit();//添加Fragment到UI
            String entryName = "com.qihoo360.replugin.sample.demo3.utils.TimeUtils";
            Class<?> cls = d1ClassLoader.loadClass(entryName);
            Object obj = cls.newInstance();//使用插件的Classloader获取指定Fragment实例
            Method method = cls.getDeclaredMethod("getNowString");
            String str = (String)method.invoke(obj);
            Log.i(TAG, "invokeMethod getNowString=" + str);
            Toast.makeText(MainActivity.this, "invokeMethod getNowString=" + str, Toast.LENGTH_SHORT).show();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "invokeMethod Exception:" + e.getLocalizedMessage());
            return false;
        }
        return true;
    }
}
