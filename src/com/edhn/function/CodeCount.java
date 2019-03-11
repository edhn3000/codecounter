package com.edhn.function;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.edhn.config.PropReader;

/**
 * 统计同一个目录下指定扩展名文件中的代码，包括空行数，注解数量，有效代码数
 * 根据网上流传的修改，加入了多种扩展名文件的统计
 * CodeCount
 * @author fengyq
 * @version 1.0
 *
 */
public class CodeCount {


    static List<CodeCounter> counters;

    /**
     * @param ext
     * @param counters
     * @return
     */
    private static CodeCounter getCounterByExt(String ext,
            List<CodeCounter> counters) {
        for (int i = 0; i < counters.size(); i++) {
            if (ext.equals(counters.get(i).ext))
                return counters.get(i);
        }
        return null;
    }
    
    /**
     * @param sFileName
     * @return
     */
    private static String getFileExt(String sFileName){
        int n = sFileName.indexOf('.');
        if (n != -1)
            return sFileName.substring(n, sFileName.length());
        else
            return "";
    }
	
	/**
	 * @param dir
	 * @param counters
	 */
	private static void countCodeInDir(String dir, List<CodeCounter> counters){
		File fMain = new File(dir);
		if (!fMain.exists()){
			System.out.println("dir not exist.");
		}
		File[] codeFile = fMain.listFiles();
		int i=0;
		while (i<codeFile.length){
			File fChild = codeFile[i];
			if (fChild.isDirectory())
				countCodeInDir(fChild.getAbsolutePath(), counters);
			else {
	            CodeCounter c = getCounterByExt(getFileExt(fChild.getName()), counters);
//			else if (fChild.getName().endsWith(ext)){
//			else if (fChild.getName().matches(".*\\.java$")){
	            if (c!=null)
	                parse(fChild, c);
			}
			i++;
		}
	}
	
	/**
	 * @param hintMsg
	 * @return
	 */
	private static String readOneInput(String hintMsg) {
        String input = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        System.out.println(hintMsg);
        try {
            input = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    /**
	     * 公司代码权重
            100:*.java;*.pas;*.js;summer-url.properties;summer-query.properties;*.txt;*.as;
            60:*.jsp;*.php;*.html;*.htm;*.mxml;
            20:*.sql;*.css;
            6:*.xml;
	     * */
		String dir = "";
		PropReader prop = new PropReader();
		try {
		    boolean externalConfig = false;
            String jarPath = null;
            URL path = CodeCount.class.getProtectionDomain().getCodeSource().getLocation();
            if (path != null) {
                jarPath = URLDecoder.decode(path.getPath(), "UTF-8");
            }
            if (jarPath != null) {
                File confFile = new File(jarPath, "config.properties");
                if (confFile.exists()) {
                    prop.loadPropFile(confFile, true);
                    externalConfig = true;
                    System.out.println("config=" + confFile.getAbsolutePath());
                }
            }
            if (!externalConfig)
                prop.loadPropResource("/config.properties", true);
        } catch (IOException e) {
            System.out.println("加载config.properties出错！" + e.getMessage());
        }
//		String excludeFileName = String.valueOf(System.getProperty("CodeCount.Exclude"));
		String excludeExprs = prop.getPropValue("codecount.exclude.regex");
        List<String> excludeKeys = new ArrayList<String>();
		if (excludeExprs != null && !"".equals(excludeExprs)) {
		    excludeKeys.addAll(Arrays.asList(excludeExprs.split(";")));
		}
        counters = loadCounters();

        StringBuilder supportExts = new StringBuilder();
        for (int i = 0; i < counters.size(); i++) {
            counters.get(i).setExcludeRegexs(excludeKeys);
            // exclude key
//              counters.get(i).AddExcludePathKey("artery"); 
//              counters.get(i).AddExcludePathKey("summer");
            supportExts.append(counters.get(i).getExt());
            if (i < counters.size() - 1) {
                supportExts.append(", ");
            }

        }
		if (args.length != 0){
			dir = args[0];
		}else{
	        System.out.println("this tool can count code of source files in a dir ");
	        System.out.println("supported ext including " + supportExts.toString());
		    dir = readOneInput("please input a source dir:");
		}
		System.out.println("excludeExprs=" + excludeExprs);
		System.out.println("counting code in dir:" + dir);
		
		countCodeInDir(dir, counters);

		// show not find exts
//        for (int i = 0; i < counters.size(); i++) {
//            if (counters.get(i).getFileCount() == 0) {
//                System.out.println("未找到扩展名为" + counters.get(i).ext + "的文件。");
//            }
//        }
		if (counters.size() > 0) {
		    System.out.println("========统计结果========");
		}
        // show found exts
        for (int i = 0; i < counters.size(); i++) {
            if (counters.get(i).getFileCount() > 0) {
                System.out.println("扩展名为" + counters.get(i).ext + "的文件中代码统计结果如下：");
                System.out.println("代码行:= " + counters.get(i).getNormalLines() + "（系数折算行：" + counters.get(i).getFacorNormalLines() + "）");
                System.out.println("注释行:= " + counters.get(i).commenLines);
                System.out.println("空白行:= " + counters.get(i).whiteLines);
                System.out.println("总计:= " + counters.get(i).getTotalLines());
                System.out.println("统计文件个数:= " + counters.get(i).getFileCount());
            }
        }

	}

    /**
     * @return
     * @throws IOException 
     * @throws DocumentException 
     */
    private static List<CodeCounter> loadCounters() throws IOException, DocumentException {
//      counters = new CodeCounter[]{
//                new CodeCounter(".java", "//", "/*", "*/", 1),
//                new CodeCounter(".pas", "//", "{", "}", 1),
//                new CodeCounter(".js", "//", "/*", "*/", 1),
//                new CodeCounter(".jsp", null, "<!--", "-->", 0.6),
//                new CodeCounter(".htm", null, "{", "}", 0.6),
//                new CodeCounter(".xml", null, "{", "}", 0.06),
//                new CodeCounter(".sql", "--", "/*", "*/", 0.2),
//                new CodeCounter(".properties", "#", null, null, 0.2)
//                };
        
		InputStream is = CodeCount.class.getResourceAsStream("/codecounter.xml");
		if (is == null) {
		    throw new IOException("codecounter.xml丢失，无法继续处理！");
		}
		List<CodeCounter> counters = new ArrayList<CodeCounter>();
	    SAXReader reader = new SAXReader();
	    try {
            Document doc = reader.read(is);
            Element root = doc.getRootElement();
            @SuppressWarnings("unchecked")
            List<Element> counterNodeList = root.elements("CodeCounter");
            for (Element node: counterNodeList) {
                String factorStr = node.attributeValue("factor");
                double factor = 1;
                if (factorStr != null && !"".equals(factorStr)) {
                    factor = Double.parseDouble(factorStr);
                }
                CodeCounter counter = new CodeCounter(node.attributeValue("ext"),
                        node.attributeValue("comment"),
                        node.attributeValue("commentBegin"),
                        node.attributeValue("commentEnd"), factor);
                counters.add(counter);

            }
        } catch (DocumentException e) {
            System.out.println("解析codecounter.xml出错，请检查配置！" + e.getMessage());
            throw e;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		return counters;
    }

	/**
	 * 统计代码文件中的注释行、空行、有效行。
	 * 注释行、空行有规律可循，有效行即统计非前两种的情况
	 * @param f 文件操作对象
	 * @param counter 一个计数对象，参照类定义
	 */
	private static void parse(File f, CodeCounter counter) {
		BufferedReader br = null;
		boolean comment = false;
		if (counter.isExcludePath(f.getAbsolutePath()))
		    return;
		try {
			br = new BufferedReader(new FileReader(f));
			String line = "";
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.matches("^[\\s&&[^\\n]]*$")) {
				    counter.whiteLines++;
				} else if (counter.hasMultiLineComment() 
				        && line.startsWith(counter.multiLineCommentBegin) 
				        && !line.endsWith(counter.multiLineCommentEnd)) {
				    counter.commenLines++;
					comment = true;
				}else if (counter.hasMultiLineComment() 
                        && line.startsWith(counter.multiLineCommentBegin) 
				        && line.endsWith(counter.multiLineCommentEnd)) {
				    counter.commenLines++;
				}
				else if (true == comment) {
				    counter.commenLines++;
					if (counter.hasMultiLineComment() 
					  && line.endsWith(counter.multiLineCommentEnd)) {
						comment = false;
					}
				} else if (counter.hasSingleComment() 
				         && line.startsWith(counter.singleLineComment)){
				    counter.commenLines++;
				}
				else {
				    counter.normalLines++;
				}
			}
			counter.setFileCount(counter.getFileCount() + 1);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

    public static class CodeCounter {
        // 扩展名
        private String ext;
        /** 必须为每种Counter指定注释关键字 */
        public String singleLineComment;
        public String multiLineCommentBegin;
        public String multiLineCommentEnd;
        public List<Pattern> excludePatterns;
        
        /** 与标准代码相比的代码计算系数，比如jsp使用0.6 */
        private double factor = 1;
        
        /** 每种Counter有自己的计数器，即下面的3个属性*/
        public long normalLines = 0;
        public long commenLines = 0;
        public long whiteLines = 0;

        private int fileCount;

        public CodeCounter(String ext, String singleLineComment,
                String multiLineCommentBegin, String multiLineCommentEnd,
                double factor) {
            this.ext = ext;
            this.singleLineComment = singleLineComment;
            this.multiLineCommentBegin = multiLineCommentBegin;
            this.multiLineCommentEnd = multiLineCommentEnd;
            this.factor = factor;
            excludePatterns = new ArrayList<Pattern>();
        }
        
        public double getTotalLines() {
            return normalLines + commenLines + whiteLines;
        }

        public double getNormalLines() {
            return normalLines;
        }

        public double getFacorNormalLines() {
            return normalLines * factor;
        }

        public boolean hasMultiLineComment() {
            return (multiLineCommentBegin != null && !"".equals(multiLineCommentBegin))
                    && (multiLineCommentEnd != null && !"".equals(multiLineCommentEnd));
        }

        public boolean hasSingleComment() {
            return (singleLineComment != null && !"".equals(singleLineComment));
        }

        public boolean isExcludePath(String sPath) {
            for (int i = 0; i < excludePatterns.size(); i++) {
                Pattern p = excludePatterns.get(i);
                if (p.matcher(sPath).find()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the fileCount
         */
        public int getFileCount() {
            return fileCount;
        }

        /**
         * @param fileCount the fileCount to set
         */
        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }

        /**
         * @return the ext
         */
        public String getExt() {
            return ext;
        }

        /**
         * @param ext the ext to set
         */
        public void setExt(String ext) {
            this.ext = ext;
        }

        /**
         * @return the commenLines
         */
        public long getCommenLines() {
            return commenLines;
        }

        /**
         * @param commenLines the commenLines to set
         */
        public void setCommenLines(long commenLines) {
            this.commenLines = commenLines;
        }

        /**
         * @param excludeRegexs
         */
        public void setExcludeRegexs(List<String> excludeRegexs) {
            excludePatterns.clear();
            for (String regex: excludeRegexs) {
                try {
                    Pattern p = Pattern.compile(regex);
                    excludePatterns.add(p);
                } catch (RuntimeException e) {
                    System.out.println("表达式" + regex + "初始化出错！"  + e.getMessage());
                }
            }
        }
    }

}
