package co.epitre.aelf_lectures.bible;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

public class BibleChapterPagerAdapter extends FragmentPagerAdapter {
    private BibleBookEntry mBibleBookEntry;
    private int mHighlightChapterId;
    private String mHighlightQuery;
    private String mReference;

    public BibleChapterPagerAdapter(@NonNull FragmentManager fm, @NonNull BibleBookEntry bibleBookEntry, int highlightChapterId, String highlightQuery, String reference) {
        super(fm);
        mBibleBookEntry = bibleBookEntry;
        mHighlightChapterId = highlightChapterId;
        mHighlightQuery = highlightQuery;
        mReference = reference;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        BibleBookChapter chapter = mBibleBookEntry.getChapter(position);
        Fragment fragment = new BibleChapterFragment();

        Bundle args = new Bundle();
        args.putString(BibleChapterFragment.ARG_TEXT_HTML, chapter.getContent());
        if (position == mHighlightChapterId) {
            args.putString(BibleChapterFragment.ARG_HIGHLIGHT, mHighlightQuery);
        }
        args.putString(BibleChapterFragment.ARG_CHAPTER, chapter.getChapterRef());
        args.putString(BibleChapterFragment.ARG_REFERENCE, mReference);
        fragment.setArguments(args);
        return fragment;
    }

    private BibleBookChapter getChapter(int position) {
        List<BibleBookChapter> chapters = mBibleBookEntry.getChapters();

        // Handle empty state
        if (chapters.isEmpty()) {
            return null;
        }

        // Clamp position
        if (position < 0) {
            position = 0;
        }
        if (position >= chapters.size()) {
            position = chapters.size() - 1;
        }

        // Grab the chapter
        return chapters.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        BibleBookChapter chapter = getChapter(position);
        if (chapter == null) {
            return "";
        }
        return chapter.getChapterName();
    }

    public String getRoute(int position) {
        BibleBookChapter chapter = getChapter(position);
        if (chapter == null) {
            return "";
        }
        return chapter.getRoute();
    }

    @Override
    public int getCount() {
        return mBibleBookEntry.getChapters().size();
    }
}
