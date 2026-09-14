package io.github.appunnim.businessgate.data;

import android.content.Context;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Offline factual names, with provenance; no runtime lookup or identity assertions. */
public final class SenderCatalogue {
    public record Entry(String name,String source,String contact,String checkedOn) {}
    public static Map<String,Entry> load(Context context) {
        try(var input=context.getAssets().open("indian-sender-names.json")) {
            var buffer=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;
            while((count=input.read(chunk))!=-1){if(buffer.size()+count>128_000)throw new IllegalArgumentException("CATALOGUE_SIZE");buffer.write(chunk,0,count);}
            byte[] bytes=buffer.toByteArray();
            var root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
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
    private SenderCatalogue() {}
}
