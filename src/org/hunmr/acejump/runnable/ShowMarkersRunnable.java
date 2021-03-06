package org.hunmr.acejump.runnable;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShowMarkersRunnable implements Runnable {
    public static final char INFINITE_JUMP_CHAR = 'f';
    private final String _markerCharSet =     "abcdeghiyklmnopqrstuvwj,"; // / .
    private final String _fullMarkerCharSet = "abcdeghiyklmnopqrstuvwj,"; // / .
    private final List<Integer> _offsets;
    private final AceJumpAction _action;
    private final Editor _editor;
    private MarkerCollection _markerCollection;

    public ShowMarkersRunnable(List<Integer> offsets, AceJumpAction currentExecutingAction) {
        _offsets = offsets;
        _editor = currentExecutingAction.getEditor();
        this._action = currentExecutingAction;
        _markerCollection = _action.getMarkerCollection();
    }

    @Override
    public void run() {
        if (_offsets.isEmpty()) {
            return;
        }

        int caretOffset = _editor.getCaretModel().getOffset();
        sortOffsetsByDistanceToCaret(caretOffset);
        sortOffsetsToImprovePriorityOfLineEnd(caretOffset);

        int twiceJumpGroupCount = calcTwiceJumpGroupCount();
        int singleJumpCount = Math.min(getMarkerSet().length() - twiceJumpGroupCount, _offsets.size());

        createSingleJumpMarkers(singleJumpCount);
        if (twiceJumpGroupCount > 0) {
            createMultipleJumpMarkers(singleJumpCount, twiceJumpGroupCount);
        }

        _action.showNewMarkersPanel(new MarkersPanel(_editor, _markerCollection));
    }

    private String getMarkerSet() {
        return _action.isCalledFromOtherAction() ? _fullMarkerCharSet : _markerCharSet;
    }

    private void createSingleJumpMarkers(int singleJumpCount) {
        for (int i = 0; i < singleJumpCount ; i++) {
            _markerCollection.addMarker(getMarkerSet().charAt(i), _offsets.get(i));
        }
    }

    private void createMultipleJumpMarkers(int singleJumpCount, int groupsNeedsTwiceJump) {
        int i = singleJumpCount;

        int maxMarkersCountNeedsTwiceJump = Math.min(_offsets.size(), groupsNeedsTwiceJump * getMarkerSet().length());
        for (;i < maxMarkersCountNeedsTwiceJump; i++) {
            int group = (i - singleJumpCount) / getMarkerSet().length();
            char markerChar = getMarkerSet().charAt(singleJumpCount + group);
            _markerCollection.addMarker(markerChar, _offsets.get(i));
        }

        boolean hasMarkersNeedMoreJumps = i < _offsets.size();
        if (hasMarkersNeedMoreJumps) {
            for (; i < _offsets.size(); i++) {
                _markerCollection.addMarker(INFINITE_JUMP_CHAR, _offsets.get(i));
            }
        }
    }

    private int calcTwiceJumpGroupCount() {
        int makerCharSetSize = getMarkerSet().length();

        for (int groupsNeedMultipleJump = 0; groupsNeedMultipleJump < makerCharSetSize; groupsNeedMultipleJump++) {
            int oneJumpMarkerCount = makerCharSetSize - groupsNeedMultipleJump;
            if (groupsNeedMultipleJump * makerCharSetSize + oneJumpMarkerCount > _offsets.size()) {
                return groupsNeedMultipleJump;
            }
        }

        return makerCharSetSize;
    }

    private void sortOffsetsByDistanceToCaret(final int caretOffset) {
        Collections.sort(_offsets, new Comparator<Integer>() {
            @Override
            public int compare(Integer oA, Integer oB) {
                return Math.abs(oA - caretOffset) - Math.abs(oB - caretOffset);
            }
        });
    }

    private void sortOffsetsToImprovePriorityOfLineEnd(final int caretOffset) {
        Collections.sort(_offsets, new Comparator<Integer>() {
            @Override
            public int compare(Integer oA, Integer oB) {
                Document document = _editor.getDocument();
                boolean oAIsLineEndOffset = isLineEndOffset(oA, document);
                boolean oBIsLineEndOffset = isLineEndOffset(oB, document);

                if (!(oAIsLineEndOffset ^ oBIsLineEndOffset)) {
                    return 0;
                }

                return oAIsLineEndOffset ? -1 : 1;
            }

            private boolean isLineEndOffset(Integer oA, Document document) {
                int lineA = document.getLineNumber(oA);
                int lineEndOffset = document.getLineEndOffset(lineA);
                return oA == lineEndOffset;
            }
        });
    }
}
