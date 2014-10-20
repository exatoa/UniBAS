package org.sel.UnifiedBTS.Core.Network;

import java.nio.charset.UnsupportedCharsetException;

import org.apache.http.entity.ContentType;

/**
 * HTTP에서 제공되는 ContentType이 binary파일을 받아올 경우
 * 에러가 발생하여 이를 방지하기위해 확장된 ContentType을 생성.
 * @author Zeck
 *
 */
public class ContentTypeExtended{
	
	public String Type;
	public String CharSet;
	
	private ContentTypeExtended()
	{
		Type = "text/html";
		CharSet = "UTF-8";
	}
	

	public static ContentTypeExtended getContentType(String _str)//getContentType().toString()
	{
		ContentTypeExtended ctype = new ContentTypeExtended();
		if(_str==null) return ctype;
		if(_str=="") return ctype;
		
		ContentType otype = null;
		try{
			otype = org.apache.http.entity.ContentType.parse(_str);
		}
		catch(UnsupportedCharsetException ue)
		{
			
		}
		
		if (otype!=null)
		{
			if (otype.getMimeType()!=null)
				ctype.Type = otype.getMimeType();
			if (otype.getCharset()!=null)
				ctype.CharSet = otype.getCharset().displayName();
		}
		else
		{
			//기본 타입에 대한 값 얻음.
			String[] kyes = _str.split(":")[1].split(";");		//TODO :: ;문자가 없는경우 에러가 안날까?
			ctype.Type = kyes[0].trim();
			
			//부가 문자열이 있는경우 찾아서 처리
			String[] valueSet;
			for(int i=1; i<kyes.length; i++){
				valueSet = kyes[i].split("=");
				if(valueSet[0].toLowerCase().compareTo("charset")==0){
					ctype.CharSet = valueSet[1].trim();
					break;
				}
			}
		}
		
		//Content-Type이 포함되는것을 제거.
		if(ctype.Type.startsWith("Content-Type: ")==true)
			ctype.Type = ctype.Type.substring(14).trim();
		
		return ctype;
	}
}

