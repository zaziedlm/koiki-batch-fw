# Return Code Mapping

Current development line: `v0.1.0`

This is the initial project-side return code convention for JP1 and other schedulers.

| Code | Meaning | Initial Handling |
| --- | --- | --- |
| 0 | Normal completion | Treat as success. |
| 10 | Warning completion | Treat as completed with warning. Operator review may be required. |
| 20 | Business error | Treat as controlled abnormal completion caused by input, business rule, or recoverable business condition. |
| 30 | System error | Treat as abnormal completion caused by infrastructure, unexpected exception, or unrecoverable technical failure. |

## Notes

- This mapping is intentionally small at `v0.1.0`.
- JP1 job definitions should depend on these stable codes, not Java exception names.
- Detailed subcodes can be added later if operational use cases require them.
- The framework `fault` package is expected to own the future code mapping implementation.
