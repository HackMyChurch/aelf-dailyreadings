package co.epitre.aelf_lectures;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends Fragment {
	/**
	 * The fragment arguments
	 */
	public static final String ARG_DESCRIPTION = "description";
	public static final String ARG_LONG_TITLE = "long_title";

	public LectureFragment() {
	}

	@SuppressLint("NewApi") // surrounded by a runtime test 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// compute view --> HTML
		StringBuilder htmlString = new StringBuilder();
		String body = getArguments().getString(ARG_DESCRIPTION)
				.replace(" :", "&nbsp;:")
				.replace(" !", "&nbsp;!")
				.replace(" ?", "&nbsp;?")
				.replace(" &raquo;", "&nbsp;&raquo;")
				.replace("&laquo; ", "&laquo;&nbsp;");
		htmlString.append("<html><head><style type=\"text/css\">body{margin:24px}</style></head><body>");
		htmlString.append("<h3>" + getArguments().getString(ARG_LONG_TITLE) + "</h3>");
		htmlString.append(body);
		htmlString.append("</body></html>");

		// actual UI refresh
		View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
		WebView lectureView = (WebView) rootView.findViewById(R.id.LectureView);
		lectureView.getSettings().setBuiltInZoomControls(true);
		lectureView.loadDataWithBaseURL("file:///android_asset/", htmlString.toString(), "text/html", "utf-8", null);
		lectureView.setBackgroundColor(0x00000000);
		if(android.os.Build.VERSION.SDK_INT > 11)
		{
			lectureView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
			lectureView.getSettings().setDisplayZoomControls(false);
		}
		// return
		return rootView;
	}
}
