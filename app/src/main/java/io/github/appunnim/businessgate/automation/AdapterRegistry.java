package io.github.appunnim.businessgate.automation;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Only bundled, exact-environment recipes can qualify. No runtime imports or scripts. */
public final class AdapterRegistry {
    private final List<JSONObject> recipes = new ArrayList<>();
    private boolean valid = true;
    public AdapterRegistry(Context context) {
        try (var stream=context.getAssets().open("adapters/compatibility.json")) {
            JSONObject root=new JSONObject(new String(io.github.appunnim.businessgate.support.Bytes.read(stream),StandardCharsets.UTF_8));
            if(root.getInt("schemaVersion")!=1) throw new IllegalStateException("BAD_REGISTRY");
            JSONArray rows=root.getJSONArray("adapters");
            for(int i=0;i<rows.length();i++) {
                JSONObject row=rows.getJSONObject(i);
                if(!row.getBoolean("physicallyQualified") || !row.getString("evidenceSha256").matches("[a-f0-9]{64}")
                    || row.getString("reviewer").trim().isEmpty() || !row.getBoolean("interruptionSafe")
                    || !row.getBoolean("readStatePreserving")) throw new IllegalStateException("UNQUALIFIED_RECIPE");
                // Parsing validates every required path before the recipe can enter the registry.
                new QualifiedAdapter(row);
                recipes.add(row);
            }
        }catch(Exception error){recipes.clear();valid=false;}
    }
    public boolean available(){return valid&&!recipes.isEmpty();}
    public String summary(){return available()?"A bundled integration is available. Connect the tested receiving app to check this device.":"No connected-app build is qualified in this version. Your choices stay saved. No connected-app actions can run.";}
    public QualifiedAdapter resolve(Context context,String packageName) {
        if(packageName==null||!available())return null;
        try {
            String packageDigest=sha256(packageName.getBytes(StandardCharsets.UTF_8));
            PackageInfo info=context.getPackageManager().getPackageInfo(packageName,PackageManager.GET_SIGNING_CERTIFICATES);
            if(info.signingInfo==null||info.signingInfo.hasMultipleSigners())return null;
            String signer=sha256(info.signingInfo.getApkContentsSigners()[0].toByteArray());
            for(JSONObject row:recipes) {
                if(!row.getString("packageSha256").equals(packageDigest) || row.getLong("versionCode")!=info.getLongVersionCode()
                    || row.getInt("api")!=Build.VERSION.SDK_INT || !row.getString("locale").equals(Locale.getDefault().toLanguageTag()))continue;
                boolean trusted=false;
                JSONArray hashes=row.getJSONArray("signingSha256");
                for(int i=0;i<hashes.length();i++)if(signer.equals(hashes.getString(i)))trusted=true;
                if(trusted)return new QualifiedAdapter(row);
            }
        }catch(Exception ignored){return null;}
        return null;
    }
    public static String sha256(byte[] bytes) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
        }catch(Exception error){throw new IllegalStateException("DIGEST_UNAVAILABLE");}
    }
}
