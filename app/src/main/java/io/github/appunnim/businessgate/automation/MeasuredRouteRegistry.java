package io.github.appunnim.businessgate.automation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit experimental support, restricted to the measured emulator installation and display. */
final class MeasuredRouteRegistry {
    private final JSONArray rows;
    MeasuredRouteRegistry(Context context, JSONObject registry) throws Exception {
        rows=registry.has("measuredRoutes")?registry.getJSONArray("measuredRoutes"):new JSONArray();
        if(rows.length()>1)throw new IllegalArgumentException("AMBIGUOUS_ROUTE");
        for(int i=0;i<rows.length();i++){
            JSONObject r=rows.getJSONObject(i);
            if(!r.getString("id").equals("business-profile-v1")||!r.getString("supportLevel").equals("emulator-experimental")
                ||r.getBoolean("physicalQualification")||!r.getBoolean("foregroundSessionRequired")||r.getInt("api")!=36)
                throw new IllegalArgumentException("INVALID_MEASURED_ROUTE");
            for(String field:new String[]{"packageSha256","evidenceSha256","businessRouteSha256","receiverRouteSha256","actionEchoSha256"})digest(r.getString(field));
            if(r.getLong("versionCode")<1)throw new IllegalArgumentException("INVALID_BUILD");
            for(String field:new String[]{"signingSha256","signingHistorySha256"}){
                JSONArray set=r.getJSONArray(field);if(set.length()<1||set.length()>8)throw new IllegalArgumentException("INVALID_SIGNERS");
                java.util.Set<String> seen=new java.util.HashSet<>();for(int j=0;j<set.length();j++){digest(set.getString(j));if(!seen.add(set.getString(j)))throw new IllegalArgumentException("DUPLICATE_SIGNER");}
            }
            JSONObject env=r.getJSONObject("environment");
            if(!env.getString("model").equals("sdk_gphone64_arm64")||!env.getString("manufacturer").equals("Google")
                ||!env.getString("deviceLocale").equals("en-US")||env.getInt("fontScalePercent")!=100||env.getInt("densityDpi")!=420||env.getInt("orientation")!=1)
                throw new IllegalArgumentException("UNMEASURED_ENVIRONMENT");
            byte[] bytes=asset(context,"adapters/evidence/business-profile-v1.json");
            if(!AdapterRegistry.sha256(bytes).equals(r.getString("evidenceSha256")))throw new IllegalArgumentException("EVIDENCE_CHANGED");
            JSONObject evidence=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            JSONObject contract=new JSONObject(r.toString());contract.remove("evidenceSha256");
            JSONObject measuredEnvironment=new JSONObject();for(String key:new String[]{"packageSha256","versionCode","api","signingSha256","signingHistorySha256","environment"})measuredEnvironment.put(key,r.get(key));
            if(!AdapterRegistry.canonical(measuredEnvironment).equals(AdapterRegistry.canonical(evidence.getJSONObject("environment"))))throw new IllegalArgumentException("ENVIRONMENT_EVIDENCE_MISMATCH");
            if(!evidence.getString("contractSha256").equals(AdapterRegistry.sha256(AdapterRegistry.canonical(contract).getBytes(StandardCharsets.UTF_8)))
                ||evidence.getBoolean("physicalQualification")||!evidence.getJSONObject("block").getBoolean("blockedStateVerified")
                ||!evidence.getJSONObject("unblock").getBoolean("unblockedStateVerified")||!evidence.getBoolean("receiverIdentitySyntaxVerified")
                ||!evidence.getBoolean("receivingAccountBindingVerified")||!evidence.getBoolean("nativeNameControlsVerified")||!evidence.getBoolean("stopControlVerified")
                ||evidence.getInt("liveAssertions")<25||!evidence.getString("finalObservedState").equals("BLOCKED"))throw new IllegalArgumentException("ROUTE_EVIDENCE_MISMATCH");
            if(java.time.LocalDate.parse(evidence.getString("capturedOn")).isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC)))throw new IllegalArgumentException("FUTURE_EVIDENCE");
            JSONArray artifacts=evidence.getJSONArray("artifacts");if(artifacts.length()<1||artifacts.length()>4)throw new IllegalArgumentException("MISSING_RESULTS");
            for(int j=0;j<artifacts.length();j++){
                JSONObject a=artifacts.getJSONObject(j);String path=a.getString("path");
                if(!path.matches("adapters/evidence/[a-z0-9-]+\\.txt")||!AdapterRegistry.sha256(asset(context,path)).equals(a.getString("sha256")))throw new IllegalArgumentException("RESULTS_CHANGED");
            }
        }
    }
    boolean available(Context context){
        if(rows.length()==0)return false;
        for(var activity:context.getPackageManager().queryIntentActivities(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),0))
            if(resolve(context,activity.activityInfo.packageName)!=null)return true;
        return false;
    }
    String resolve(Context context,String pkg){
        if(pkg==null||pkg.isEmpty())return null;
        try{
            if(Build.VERSION.SDK_INT<36||context.getSystemService(android.os.UserManager.class).isManagedProfile())return null;
            var info=context.getPackageManager().getPackageInfo(pkg,PackageManager.GET_SIGNING_CERTIFICATES);
            if(info.signingInfo==null||info.signingInfo.hasMultipleSigners()||info.signingInfo.getApkContentsSigners().length!=1)return null;
            var config=context.getResources().getConfiguration();
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i),e=r.getJSONObject("environment");
                if(!r.getString("packageSha256").equals(AdapterRegistry.sha256(pkg.getBytes(StandardCharsets.UTF_8)))||info.getLongVersionCode()!=r.getLong("versionCode")
                    ||Build.VERSION.SDK_INT!=r.getInt("api")||!Build.MODEL.equals(e.getString("model"))||!Build.MANUFACTURER.equals(e.getString("manufacturer"))
                    ||!config.getLocales().get(0).toLanguageTag().equals(e.getString("deviceLocale"))||Math.round(config.fontScale*100)!=e.getInt("fontScalePercent")
                    ||config.densityDpi!=e.getInt("densityDpi")||config.orientation!=e.getInt("orientation"))continue;
                if(!contains(r.getJSONArray("signingSha256"),AdapterRegistry.sha256(info.signingInfo.getApkContentsSigners()[0].toByteArray())))continue;
                boolean history=true;for(var signature:info.signingInfo.getSigningCertificateHistory())if(!contains(r.getJSONArray("signingHistorySha256"),AdapterRegistry.sha256(signature.toByteArray())))history=false;
                if(history)return r.getString("id");
            }
        }catch(Exception unsupported){return null;}
        return null;
    }
    private static void digest(String s){if(!s.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("INVALID_DIGEST");}
    private static boolean contains(JSONArray set,String s)throws Exception{for(int i=0;i<set.length();i++)if(set.getString(i).equals(s))return true;return false;}
    private static byte[] asset(Context context,String path)throws Exception{
        try(var stream=context.getAssets().open(path)){byte[] bytes=io.github.appunnim.businessgate.support.Bytes.read(stream);if(bytes.length>128_000)throw new IllegalArgumentException("UNBOUNDED_EVIDENCE");return bytes;}
    }
}
