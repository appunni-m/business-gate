package io.github.appunnim.businessgate.data;

import android.content.Context;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;

/** Offline displayed-name rules with explicit provenance; no runtime lookup or identity assertions. */
public final class SenderCatalogue {
    public record Entry(String name,String source,String contact,String checkedOn) {}
    public static Set<String> loadSupplied(Context context) {
        try {
            var root=read(context,"supplied-sender-rules.json");
            if(root.getInt("schemaVersion")!=1||!root.getString("source").equals("owner-provided")||!root.getString("encoding").equals("base64-utf8-json"))throw new IllegalArgumentException("SUPPLIED_RULE_SCHEMA");
            String payload=root.getString("payload");
            byte[] decoded=android.util.Base64.decode(payload,android.util.Base64.NO_WRAP);
            if(!android.util.Base64.encodeToString(decoded,android.util.Base64.NO_WRAP).equals(payload))throw new IllegalArgumentException("SUPPLIED_RULE_ENCODING");
            var rows=new JSONArray(new String(decoded,StandardCharsets.UTF_8));
            if(rows.length()<1||rows.length()>500)throw new IllegalArgumentException("SUPPLIED_RULE_SIZE");
            Set<String> names=new HashSet<>();
            for(int i=0;i<rows.length();i++){
                String raw=rows.getString(i),name=NameVisibilityPolicy.nameKey(raw);
                if(!raw.equals(name)||!names.add(name))throw new IllegalArgumentException("SUPPLIED_RULE_NAME");
            }
            return Set.copyOf(names);
        }catch(java.io.IOException|org.json.JSONException invalid){throw new IllegalStateException("SUPPLIED_RULE_UNAVAILABLE",invalid);}
    }
    public static Map<String,Entry> load(Context context) {
        try {
            var root=read(context,"indian-sender-names.json");
            if(root.getInt("schema")!=1)throw new IllegalArgumentException("CATALOGUE_SCHEMA");
            var rows=root.getJSONArray("entries");if(rows.length()>500)throw new IllegalArgumentException("CATALOGUE_SIZE");
            Map<String,Entry> entries=new LinkedHashMap<>();
            for(int i=0;i<rows.length();i++) {
                var row=rows.getJSONObject(i);String name=NameVisibilityPolicy.nameKey(row.getString("name"));
                String source=row.getString("profileSource"),contact=row.getString("contactSource");
                if(!row.getString("country").equals("IN")||!row.getString("evidence").equals("public-profile")||!source.matches("https://wa[.]me/91[0-9]{10}")||!contact.startsWith("https://")||entries.containsKey(name))throw new IllegalArgumentException("CATALOGUE_ENTRY");
                entries.put(name,new Entry(name,source,contact,root.getString("checkedOn")));
            }
            return Map.copyOf(entries);
        }catch(java.io.IOException|org.json.JSONException invalid){throw new IllegalStateException("CATALOGUE_UNAVAILABLE",invalid);}
    }
    private static JSONObject read(Context context,String file)throws java.io.IOException,org.json.JSONException {
        try(var input=context.getAssets().open(file)){
            var buffer=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;
            while((count=input.read(chunk))!=-1){if(buffer.size()+count>128_000)throw new IllegalArgumentException("CATALOGUE_SIZE");buffer.write(chunk,0,count);}
            return new JSONObject(new String(buffer.toByteArray(),StandardCharsets.UTF_8));
        }
    }
    private SenderCatalogue() {}
}
