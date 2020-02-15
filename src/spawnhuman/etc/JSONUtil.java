package spawnhuman.etc;

import org.json.simple.JSONObject;

public class JSONUtil {
	/**
	 * Uses {@link JSONObject#toString()} but with specific formatting to make it more human-readable.
	 * @param jsonObject
	 * @return
	 */
	public static String prettyPrint(JSONObject jsonObject) {
		String str = jsonObject.toJSONString();
		str = str.replace("{", "{\n");
		str = str.replace("[", "[\n");
		str = str.replace("]", "\n]");
		str = str.replace("}", "\n}");
		str = str.replace(",", ",\n");
		
		String[] strs = str.split("\n");
		int tab = 0;
		for (int i = 0; i < strs.length; i++) {
			String s = strs[i];

			if ( s.contains("}") || s.contains("]") )
				tab--;
			
			String prefix = "";
			for (int j = 0; j < tab; j++) {
				prefix += "\t";
			}
			
			strs[i] = prefix + s;
			
			if ( s.contains("{") || s.contains("[") )
				tab++;
		}
		
		StringBuilder finalString = new StringBuilder();
		for (int i = 0; i < strs.length; i++) {
			finalString.append(strs[i]);
			finalString.append("\n");
		}
		
		return finalString.toString();
	}
}
