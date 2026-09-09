package io.github.appunnim.businessgate.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Bytes {
    private Bytes(){}
    public static byte[] read(InputStream input)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=input.read(buffer))!=-1){if(out.size()+count>1_048_576)throw new IOException("ASSET_TOO_LARGE");out.write(buffer,0,count);}return out.toByteArray();
    }
}
