# Standard definitions
D DATE "Date: \w\w\w \w\w\w \d+ \d\d:\d\d:\d\d \d\d\d\d [-+]\d\d\d\d"
# A commit ID.
D COMMIT_ID "[a-f0-9]+"
# A status log header RE.  Captures the commit id in its sole group.
D COMMIT_HEAD "commit (${COMMIT_ID})[ \t]*\n(?:Merge:\s+[0-9a-f]{7}\s+[0-9a-f]{7}[ ]*\n)?${DATE}"
# A full log entry.  Captures the entry. Assume logs messages don't contain
# "==="
D COMMIT_LOG "(===[ ]*\ncommit ${COMMIT_ID}[ ]*\n(?:Merge:\s+[0-9a-f]{7}\s+[0-9a-f]{7}[ ]*\n)?${DATE}[ ]*\n(?:.|\n)*?(?=\Z|\n===))"
# An arbitrary line of text (works even with ?s)
D ARBLINE "[^\n]*(?=\n|\Z)"
# Zero or more arbitrary full lines of text.
D ARBLINES "(?:(?:.|\n)*(?:\n|\Z)|\A|\Z)"
