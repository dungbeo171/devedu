import type { SubmissionLanguage } from './types/programmingProblem'

export function createStarterCode(language: SubmissionLanguage, title: string): string {
  const task = title.trim() || 'bài tập này'

  switch (language) {
    case 'CPP':
      return `#include <bits/stdc++.h>
using namespace std;

int main() {
    // TODO: ${task}
    return 0;
}
`
    case 'JAVA':
      return `import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // TODO: ${task}
    }
}
`
    case 'PYTHON':
      return `import sys

# TODO: ${task}
def solve():
    pass

if __name__ == "__main__":
    solve()
`
    case 'HTML':
      return `<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>${task}</title>
</head>
<body>
  <!-- TODO: Hoàn thành ${task} -->
</body>
</html>
`
    case 'MYSQL':
      return `-- ${task}
-- Viết truy vấn của bạn bên dưới
SELECT *
FROM your_table;
`
  }
}
