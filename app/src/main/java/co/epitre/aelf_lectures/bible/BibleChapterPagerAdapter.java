package co.epitre.aelf_lectures.bible;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class BibleChapterPagerAdapter extends FragmentPagerAdapter {
    private BibleBookEntry mBibleBookEntry;
    private int mHighlightChapterId;
    private String mHighlightQuery;

    public BibleChapterPagerAdapter(@NonNull FragmentManager fm, @NonNull BibleBookEntry bibleBookEntry, int highlightChapterId, String highlightQuery) {
        super(fm);
        mBibleBookEntry = bibleBookEntry;
        mHighlightChapterId = highlightChapterId;
        mHighlightQuery = highlightQuery;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        String chapterText = mBibleBookEntry.getChapter(position).getContent();
        Fragment fragment = new BibleChapterFragment();

        Bundle args = new Bundle();
        args.putString(BibleChapterFragment.ARG_TEXT_HTML, chapterText);
        if (position == mHighlightChapterId) {
            args.putString(BibleChapterFragment.ARG_HIGHLIGHT, mHighlightQuery);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mBibleBookEntry.getChapters().get(position).getChapterName();
    }

    public String getRoute(int position) {
        return mBibleBookEntry.getChapters().get(position).getRoute();
    }

    @Override
    public int getCount() {
        return mBibleBookEntry.getChapters().size();
    }
}
