package expo.modules.application;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import org.unimodules.core.ExportedModule;
import org.unimodules.core.ModuleRegistry;
import org.unimodules.core.Promise;
import org.unimodules.core.interfaces.ActivityProvider;
import org.unimodules.core.interfaces.ExpoMethod;
import org.unimodules.core.interfaces.RegistryLifecycleListener;

public class ApplicationModule extends ExportedModule implements RegistryLifecycleListener {
  private static final String NAME = "ExpoApplication";
  private static final String TAG = ApplicationModule.class.getSimpleName();

  private ModuleRegistry mModuleRegistry;
  private Context mContext;
  private ActivityProvider mActivityProvider;
  private Activity mActivity;

  public ApplicationModule(Context context) {
    super(context);
    mContext = context;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mModuleRegistry = moduleRegistry;
    mActivityProvider = moduleRegistry.getModule(ActivityProvider.class);
    mActivity = mActivityProvider.getCurrentActivity();
  }

  @ExpoMethod
  public void getFirstInstallTimeAsync(Promise promise) {
    PackageManager packageManager = mContext.getPackageManager();
    String packageName = mContext.getPackageName();
    try {
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      promise.resolve((double)info.firstInstallTime);
    } catch (Exception e) {
      e.printStackTrace();
      promise.reject("ERR_APPLICATION", "Unable to get first install time of this application.", e);
    }
  }

  @ExpoMethod
  public void getLastUpdateTime(Promise promise) {
    PackageManager packageManager = mContext.getPackageManager();
    String packageName = mContext.getPackageName();
    try {
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      promise.resolve((double)info.lastUpdateTime);
    } catch (Exception e) {
      e.printStackTrace();
      promise.reject("ERR_APPLICATION", "Unable to get last update time of this application.", e);
    }
  }

  @Override
  public Map<String, Object> getConstants() {
    HashMap<String, Object> constants = new HashMap<>();


    String applicationName = mContext.getApplicationInfo().loadLabel(mContext.getPackageManager()).toString();
    String packageName = mContext.getPackageName();

    constants.put("applicationName", applicationName);
    constants.put("bundleId", packageName);

    PackageManager packageManager = mContext.getPackageManager();
    try {
      PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
      constants.put("nativeAppVersion", pInfo.versionName);
      constants.put("nativeBuildVersion", pInfo.versionCode);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    constants.put("androidId", Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
    constants.put("installReferrer", getInstallReferrer());

    return constants;
  }

  private String getInstallReferrer() {

    final StringBuilder installReferrer = new StringBuilder();

    final InstallReferrerClient referrerClient;
    referrerClient = InstallReferrerClient.newBuilder(mContext).build();
    referrerClient.startConnection(new InstallReferrerStateListener() {
      @Override
      public void onInstallReferrerSetupFinished(int responseCode) {
        switch (responseCode) {
          case InstallReferrerClient.InstallReferrerResponse.OK:
            // Connection established
            Log.d("INSTALL_REFERRER","connection established");
            try {
              ReferrerDetails response = referrerClient.getInstallReferrer();
              installReferrer.append(response.getInstallReferrer());
              response.getReferrerClickTimestampSeconds();
              response.getInstallBeginTimestampSeconds();
            } catch (RemoteException e) {
              e.printStackTrace();
            }
            break;
          case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
            // API not available on the current Play Store app
            Log.d("INSTALL_REFERRER","feature not supported");
            break;
          case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
            // Connection could not be established
            Log.d("INSTALL_REFERRER","connection could not be established");
            break;
        }
      }

      @Override
      public void onInstallReferrerServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
      }
    });
    referrerClient.endConnection();
    return installReferrer.toString();
  }


}

