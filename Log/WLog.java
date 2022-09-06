package dareesoft.riaas.ai.od;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by wslim on 2017-02-01.
 */

public final class WLog
{
	private static int		m_iLogLevel			= Log.VERBOSE;		// max output log level

	private static boolean	m_blEnable			= false;			// is Enable Log
	private static boolean	m_blExStackTrace	= false;			// is Stack Traced Exception
	private static boolean	m_blConsoleOut		= false;			// is Console Output
	private static boolean	m_blFileOut			= false;			// is File Output

	private static String	m_strLogStream		= "";
	private static long		m_lStremmingMs		= -1;

	private static String	m_strLogFile		= null;				// Log File Path
	private static Object	m_SyncFile			= new Object();

	private static Map<Integer, String> m_mapLogStr	= null;

	private WLog()														{}

	public static void SetEnable(boolean blEnable)						{	m_blEnable			= blEnable;			}
	public static void SetLogLevel(int iLogLevel)						{	m_iLogLevel			= iLogLevel;		}
	public static void SetExStackTrace(boolean blExStackTrace)			{	m_blExStackTrace	= blExStackTrace;	}
	public static void SetConsoleOut(boolean blConsoleOut)				{	m_blConsoleOut		= blConsoleOut;		}
	public static void SetFileOut(boolean blFileOut)					{	m_blFileOut			= blFileOut;		}

	public static void v(String strLog, Object... args)					{	log(Log.VERBOSE,	null,	strLog, args);	}		// Log.println only <<
	public static void v()												{	log(Log.VERBOSE,	null,	"");			}
	public static void d(String strLog, Object... args)					{	log(Log.DEBUG,		null,	strLog, args);	}
	public static void d()												{	log(Log.DEBUG,		null,	"");			}
	public static void i(String strLog, Object... args)					{	log(Log.INFO,		null,	strLog, args);	}
	public static void i()												{	log(Log.INFO,		null,	"");			}
	public static void w(String strLog, Object... args)					{	log(Log.WARN,		null,	strLog, args);	}
	public static void w()												{	log(Log.WARN,		null,	"");			}
	public static void e(String strLog, Object... args)					{	log(Log.ERROR,		null,	strLog, args);	}
	public static void e()												{	log(Log.ERROR,		null,	"");			}
//	public static void e(Throwable ex)									{	log(Log.ERROR,		ex,		"");			}
//	public static void e(Throwable ex, String strLog, Object... args)	{	log(Log.ERROR,		ex,		strLog, args);	}
	public static void a(String strLog, Object... args)					{	log(Log.ASSERT,		null,	strLog, args);	}
	public static void a()												{	log(Log.ASSERT,		null,	"");			}
	public static void a(Throwable ex)									{	log(Log.ASSERT,		ex,		"");			}
	public static void a(Throwable ex, String strLog, Object... args)	{	log(Log.ASSERT,		ex,		strLog, args);	}		// Log.println only >>

	@RequiresApi(api = Build.VERSION_CODES.N)
	private static void log(int iLogLevel, Throwable ex, String strLog, Object... args)
	{
		if( !m_blEnable || iLogLevel < m_iLogLevel )	return;

		StackTraceElement stack = Thread.currentThread().getStackTrace()[4];

		if( args.length > 0 )	strLog = String.format(strLog, args);

		String strClassName = stack.getClassName();
		int index = strClassName.lastIndexOf(".");
		strClassName = strClassName.substring(index+1);

		if( ex != null )
		{
			if( m_blExStackTrace )		strLog = strLog + " !!! " + ex.getMessage() + " !!!\n" + Log.getStackTraceString(ex);
			else						strLog = strLog + " !!! " + ex.getMessage() + " !!!";
		}

		if( m_blConsoleOut )
		{
			strLog = "[" + stack.getFileName() + ":" + stack.getLineNumber() + "] " + strClassName + "." + stack.getMethodName() + "()\t| " + strLog;

			Log.println(m_iLogLevel, "WLog", strLog);
		}

		if( m_lStremmingMs > 0 )
		{
			if( m_lStremmingMs < System.currentTimeMillis() )		EndStreamming();
		}

		if( m_blFileOut || m_lStremmingMs > 0 )
		{
			SimpleDateFormat simpleDateFormat	= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.US);

			final String		fname				= "ADAS" + simpleDateFormat.format(new Date().getTime()) + ".jpg";

			if( m_mapLogStr == null )	SetLogMap();

			strLog = 	"[" + simpleDateFormat.format(new Date().getTime()) + " " + m_mapLogStr.getOrDefault(new Integer(iLogLevel), "   ") + " " +
						stack.getFileName() + ":" + stack.getLineNumber() + "] " + strClassName + "." + stack.getMethodName() + "()\t| " + strLog;

			if( m_blFileOut ) 			logWrite(strLog);
			if( m_lStremmingMs > 0 ) 	m_strLogStream = m_strLogStream + "\n" + strLog;
		}
	}

	private static void SetLogMap()
	{
		if( m_mapLogStr != null )	return;

		m_mapLogStr = new HashMap<>();
		m_mapLogStr.put(Log.VERBOSE,	"VER");		m_mapLogStr.put(Log.DEBUG,		"DEB");		m_mapLogStr.put(Log.INFO,		"INF");
		m_mapLogStr.put(Log.WARN,		"WAR");		m_mapLogStr.put(Log.ERROR,		"ERR");		m_mapLogStr.put(Log.ASSERT,		"ASS");
	}

	private static void logWrite(String strLog)
	{
		if( m_strLogFile == null )
		{
			m_strLogFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/WLog.log";
			//m_strLogFile = "/sdcard/Dareesoft/WLog.log";
			Log.e("WLog", "m_strLogFile = " + m_strLogFile);
		}

		synchronized( m_SyncFile )
		{
			try
			{
				FileOutputStream fos = new FileOutputStream(m_strLogFile, true);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
				writer.write(strLog + "\n");
				writer.flush();
				writer.close();
				fos.close();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	private static List<String> TailFile(final int noOfLines)
	{
		if( m_strLogFile == null )													return null;
		if( android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O )	return null;

		synchronized( m_SyncFile )
		{
			try
			{
				Stream<String> stream = Files.lines(Paths.get(m_strLogFile));
				FileBuffer fileBuffer = new FileBuffer(noOfLines);
				stream.forEach(line -> fileBuffer.collect(line));
				return fileBuffer.getLines();
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	private static class FileBuffer
	{
		private int offset = 0;
		private final int noOfLines;
		private final String[] lines;

		public FileBuffer(int noOfLines)
		{
			this.noOfLines = noOfLines;
			this.lines = new String[noOfLines];
		}

		public void collect(String line)
		{
			lines[offset++ % noOfLines] = line;
		}

		public List<String> getLines()
		{
			if( Build.VERSION.SDK_INT < Build.VERSION_CODES.N )		return null;

			return IntStream.range(offset < noOfLines ? 0 : offset - noOfLines, offset).mapToObj(idx -> lines[idx % noOfLines]).collect(Collectors.toList());
		}
	}

	public static List<String> StartStreamming()
	{
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.O )
		{
			EndStreamming();
			return null;
		}

		m_lStremmingMs = System.currentTimeMillis() + 60000;
		m_strLogStream 	= "";

		return TailFile(100);
	}

	public static void EndStreamming()
	{
		m_lStremmingMs = -1;
		m_strLogStream = "";
	}

	public static String GetStreamming()
	{
		if( m_lStremmingMs < 0 )		return null;

		m_lStremmingMs = System.currentTimeMillis() + 60000;
		if( m_strLogStream.isEmpty() )	return null;

		String str = new String(m_strLogStream);

		m_strLogStream = "";
		return str;
	}

	public static String GetFullLog()
	{
		if( m_strLogFile == null )		return null;

		byte[] arrByte = new byte[32768];
		int nRead;
		synchronized( m_SyncFile )
		{
			try
			{
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				FileInputStream in = new FileInputStream(m_strLogFile);
				while( ( nRead = in.read(arrByte) ) > 0 )
				{
					byteArrayOutputStream.write(arrByte, 0, nRead);
				}

				if( byteArrayOutputStream.size() <= 0 )		return null;

				return byteArrayOutputStream.toString("utf-8");
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	public static void RemoveLogFile()
	{
		if( m_strLogFile == null )		return;

		synchronized( m_SyncFile )
		{
			File file = new File(m_strLogFile);
			file.delete();
		}
	}
}
