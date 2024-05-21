package co.epitre.aelf_lectures.lectures.data.office;

import androidx.annotation.NonNull;

import co.epitre.aelf_lectures.lectures.data.IsoDate;

public record OfficeChecksum(
        @NonNull String checksum,
        @NonNull IsoDate generationDate
) {}
