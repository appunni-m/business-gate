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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bundled contracts only. Environment, structure and reviewed evidence must agree. */
public final class AdapterRegistry {
    private final List<JSONObject> recipes=new ArrayList<>();
    private boolean valid=true;
    private MeasuredRouteRegistry measured;
    private final Context context;
    public AdapterRegistry(Context context) {
        this.context=context.getApplicationContext();
        try {
            JSONObject root=new JSONObject(new String(asset(context,"adapters/compatibility.json",512_000),StandardCharsets.UTF_8));
            if(root.getInt("schemaVersion")!=1)throw new IllegalArgumentException("BAD_REGISTRY");
            measured=new MeasuredRouteRegistry(context,root);
            JSONArray rows=root.getJSONArray("adapters");if(rows.length()>32)throw new IllegalArgumentException("REGISTRY_TOO_LARGE");
            Set<String> ids=new HashSet<>(),environments=new HashSet<>();
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.getJSONObject(i);new QualifiedAdapter(row);
                if(!ids.add(row.getString("id")))throw new IllegalArgumentException("DUPLICATE_ADAPTER");
                digest(row.getString("packageSha256"));digest(row.getString("evidenceSha256"));
                if(row.getLong("versionCode")<1||row.getInt("api")<29||!row.getBoolean("physicallyQualified")||!row.getBoolean("interruptionSafe")
                    ||!row.getBoolean("readStatePreserving")||!row.getString("locale").equals("en")||row.getString("reviewer").trim().isEmpty())throw new IllegalArgumentException("UNQUALIFIED_RECIPE");
                JSONArray signing=row.getJSONArray("signingSha256"),history=row.getJSONArray("signingHistorySha256");validateDigests(signing);validateDigests(history);
                JSONObject env=row.getJSONObject("environment");
                for(String key:new String[]{"manufacturer","model","deviceLocale"})if(env.getString(key).isEmpty()||env.getString(key).length()>160)throw new IllegalArgumentException("INVALID_ENVIRONMENT");
                if(env.getInt("fontScalePercent")<50||env.getInt("fontScalePercent")>300||env.getInt("densityDpi")<72||env.getInt("densityDpi")>1000
                    ||env.getInt("orientation")<1||env.getInt("orientation")>2)throw new IllegalArgumentException("INVALID_DISPLAY");
                String key=row.getString("packageSha256")+":"+row.getLong("versionCode")+":"+row.getInt("api")+":"+canonical(env);
                if(!environments.add(key))throw new IllegalArgumentException("AMBIGUOUS_ENVIRONMENT");
                String evidencePath="adapters/evidence/"+row.getString("id")+".json";
                byte[] bytes=asset(context,evidencePath,128_000);if(!sha256(bytes).equals(row.getString("evidenceSha256")))throw new IllegalArgumentException("EVIDENCE_CHANGED");
                JSONObject evidence=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                JSONObject contract=new JSONObject(row.toString());contract.remove("evidenceSha256");
                if(evidence.getInt("schemaVersion")!=1||!evidence.getString("contractSha256").equals(sha256(canonical(contract).getBytes(StandardCharsets.UTF_8)))
                    ||!evidence.getString("reviewer").equals(row.getString("reviewer"))||!evidence.getBoolean("physicalDevice")
                    ||!evidence.getBoolean("readReceiptsPreserved")||!evidence.getBoolean("noUnauthorizedSideEffects"))throw new IllegalArgumentException("EVIDENCE_MISMATCH");
                if(java.time.LocalDate.parse(evidence.getString("capturedOn")).isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))throw new IllegalArgumentException("FUTURE_EVIDENCE");
                JSONObject cases=evidence.getJSONObject("cases");
                for(int test=1;test<=96;test++)if(!"PASS".equals(cases.getString(String.format(Locale.ROOT,"full:T%03d",test))))throw new IllegalArgumentException("INCOMPLETE_QUALIFICATION");
                JSONArray artifacts=evidence.getJSONArray("artifacts");if(artifacts.length()<1||artifacts.length()>16)throw new IllegalArgumentException("MISSING_FIXTURES");
                for(int j=0;j<artifacts.length();j++){
                    JSONObject fixture=artifacts.getJSONObject(j);String path=fixture.getString("path");
                    if(!path.matches("adapters/evidence/[a-z0-9-]+\\.(json|txt)"))throw new IllegalArgumentException("INVALID_EVIDENCE_PATH");
                    if(!sha256(asset(context,path,256_000)).equals(fixture.getString("sha256")))throw new IllegalArgumentException("FIXTURE_CHANGED");
                }
                recipes.add(row);
            }
        }catch(Exception error){recipes.clear();measured=null;valid=false;}
    }
    private static byte[] asset(Context context,String path,int limit)throws java.io.IOException {
        try(var stream=context.getAssets().open(path);var out=new java.io.ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int count;
            while((count=stream.read(buffer))!=-1){if(out.size()+count>limit)throw new java.io.IOException("ASSET_TOO_LARGE");out.write(buffer,0,count);}return out.toByteArray();
        }
    }
    private static void digest(String value){if(!value.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("INVALID_DIGEST");}
    private static void validateDigests(JSONArray values)throws org.json.JSONException {
        if(values.length()<1||values.length()>8)throw new IllegalArgumentException("INVALID_SIGNING_SET");Set<String> unique=new HashSet<>();
        for(int i=0;i<values.length();i++){String value=values.getString(i);digest(value);if(!unique.add(value))throw new IllegalArgumentException("DUPLICATE_SIGNER");}
    }
    public boolean available(){return valid&&(!recipes.isEmpty()||(measured!=null&&measured.available(context)));}
    public String measuredRoute(Context context,String pkg){return valid&&measured!=null?measured.resolve(context,pkg):null;}
    public boolean supported(Context context,String pkg){return measuredRoute(context,pkg)!=null||resolve(context,pkg)!=null;}
    public boolean valid(){return valid;}
    public String summary(){return available()?"Experimental connected actions are available on the measured Android 36 emulator only. Select its installation and verify the receiving account. Physical devices are not yet qualified.":valid?"No connected-app build is qualified in this version. Your choices stay saved. No connected-app actions can run.":"Compatibility data did not pass validation. Actions are disabled; install a verified update.";}
    public QualifiedAdapter resolve(Context context,String packageName) {
        if(packageName==null||!available())return null;
        try {
            android.os.UserManager users=context.getSystemService(android.os.UserManager.class);
            if(Build.VERSION.SDK_INT>=30){if(users.isManagedProfile())return null;}
            else if(!users.isSystemUser())return null;
            String packageDigest=sha256(packageName.getBytes(StandardCharsets.UTF_8));
            PackageInfo info=context.getPackageManager().getPackageInfo(packageName,PackageManager.GET_SIGNING_CERTIFICATES);
            if(info.signingInfo==null||info.signingInfo.hasMultipleSigners()||info.signingInfo.getApkContentsSigners().length!=1)return null;
            String signer=sha256(info.signingInfo.getApkContentsSigners()[0].toByteArray());QualifiedAdapter match=null;
            var configuration=context.getResources().getConfiguration();
            for(JSONObject row:recipes){
                JSONObject env=row.getJSONObject("environment");
                if(!row.getString("packageSha256").equals(packageDigest)||row.getLong("versionCode")!=info.getLongVersionCode()||row.getInt("api")!=Build.VERSION.SDK_INT
                    ||!env.getString("manufacturer").equals(Build.MANUFACTURER)||!env.getString("model").equals(Build.MODEL)
                    ||!env.getString("deviceLocale").equals(configuration.getLocales().get(0).toLanguageTag())
                    ||env.getInt("fontScalePercent")!=Math.round(configuration.fontScale*100)||env.getInt("densityDpi")!=configuration.densityDpi
                    ||env.getInt("orientation")!=configuration.orientation||!contains(row.getJSONArray("signingSha256"),signer))continue;
                boolean history=true;for(var signature:info.signingInfo.getSigningCertificateHistory())if(!contains(row.getJSONArray("signingHistorySha256"),sha256(signature.toByteArray())))history=false;
                if(history){if(match!=null)return null;match=new QualifiedAdapter(row);}
            }
            // Connected UI language is proven by exact measured structural labels, never device language alone.
            return match;
        }catch(Exception ignored){return null;}
    }
    private static boolean contains(JSONArray values,String value)throws org.json.JSONException {for(int i=0;i<values.length();i++)if(value.equals(values.getString(i)))return true;return false;}
    public static String canonical(Object value)throws org.json.JSONException {
        if(value instanceof JSONObject object){List<String> keys=new ArrayList<>();object.keys().forEachRemaining(keys::add);java.util.Collections.sort(keys);StringBuilder result=new StringBuilder("{");for(String key:keys){if(result.length()>1)result.append(',');result.append(JSONObject.quote(key)).append(':').append(canonical(object.get(key)));}return result.append('}').toString();}
        if(value instanceof JSONArray array){StringBuilder result=new StringBuilder("[");for(int i=0;i<array.length();i++){if(i>0)result.append(',');result.append(canonical(array.get(i)));}return result.append(']').toString();}
        if(value instanceof String text)return JSONObject.quote(text);return String.valueOf(value);
    }
    public static String sha256(byte[] bytes) {
        try {byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
        catch(Exception error){throw new IllegalStateException("DIGEST_UNAVAILABLE");}
    }
}
