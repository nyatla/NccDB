
public class Logger
{
	private static String _log_text=null;
	public static void log_start_line()
	{
		_log_text=new String();
	}
	public static void log_end_line()
	{
		System.out.println(_log_text);
		_log_text=null;
	}
	public static void log(String s)
	{
		if(_log_text==null){
			System.out.println(s);
		}else{
			_log_text+=s;
		}
	}

}
