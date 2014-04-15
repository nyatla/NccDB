import java.util.Date;


public class test {
	public static void main(String[] args)
	{
		String[] t1={"-cmd","init"};
		String[] t2={
			"-cmd","coin_addlist",
			"-url","1,2,3",
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
		String[] t7={
				"-cmd","srv_importcsv",
		};
		String[] t8={
				"-cmd","srv_exportcsv","-service",
				"EXCHANGE@https://coinex.pw/,EXCHANGE@https://coinedup.com/,EXCHANGE@https://www.coins-e.com/,EXCHANGE@https://cryptorush.in/,EXCHANGE@https://www.cryptsy.com/"
		};
		String[] t9={
				"-cmd","cct_html_cache","-cct_db","d:\\html_cache.db",
				"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
				"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJxFk0lyBDEIBP_S5zmIHfw1h_9uYFpwzagqsen3caN4foBCXFwQgz9JGJagNMEhINhEL4HwKFwiPMSpXT7JoEFFYpJBDJL4uYTNJTVxiIaoQxNfAlGE18XuRWRdZJ0jOgShNbouOF0zn0s0qq84Nhp1syYxJAtqMvWocmqQXCcHTqo-D_GxIcBcXC58XWZOWNNAfF1GgDVD0Reo5Kg0JYpvjJywaiuJDtF6ypVgiMSXxBKqdekdT5IsNkku8hL3XimwX2IgpYGQIaeTYZPVxVtzhqhlExYxySrayfc0krBKv7U5xNAaXFwCpUGY3hUlX6_7HAJaLqTVfCtE8iVcXBWirOtAu3TqkehTRR2X5E0WsdV4fwKCJdZdkM4u6MBpMhrsc85DnwpRqOqhTUbG_nC3i1xcBJP8_QN64bWB; _ga=GA1.2.814650664.1394343048; session_id=698a4c8561fe71ade1d589a643201a6e; modtids=%2C; cf_clearance=232ef578a69046af53ac3f268534fd186128f6b1-1395888866-2700"
		};
		String[] t10={
				"-cmd","cct_import_urls",
				"-cct_db","d:\\html_cache.db"
		};
		
		String[] test=t2;
		Main.main(test);
	/*	
		String[] s={
			"Block Crawler",
			"http://bter.com/tradeaaa"
		};
		for(int i=0;i<s.length;i++){
			System.out.println(s[i].matches("Block(chain)? ((Crawler)|(Explorers?)).*"));
		}*/
	}

	/**
	 * @param args
	 */
/*	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.print((new Date(1392526205*1000L)).toString());
	}
*/
}
