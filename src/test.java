import java.util.Date;


public class test {
	public static void main(String[] args)
	{
		String[] t1={"-cmd","init"};
		String[] t2={
			"-cmd","coin_addlist",
			"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
			"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJyrVjIxtzRUsjI0tjQxNjYyMDbQUTI3tjCBipgYGxqa1AJcMJLgB8k%2C; _ga=GA1.2.814650664.1394343048; cf_clearance=d7a067ee66c260422daf6af24422fdebc40c1aaa-1394362841-7200; session_id=4a6e4776f0fdcff2a6aab1af788a307d; modtids=%2C"};
		String[] t3={
				"-cmd","coin_exportcsv",
		};
		String[] t4={
				"-cmd","coin_importcsv",
		};
		String[] t5={
				"-cmd","url_exportcsv",
		};
		String[] t6={
				"-cmd","url_importcsv",
		};
		String[] test=t5;
		Main.main(test);
	}

	/**
	 * @param args
	 *//*
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.print((new Date(1394210528*1000L)).toString());
	}
*/
}
