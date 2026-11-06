# AI use write-up

## Tool used
Claude Opus 5.5 by Anthropic, used through the Claude app in November 2026.

## How it was used
**Brainstorming.** I used Claude to talk through ideas for the POE, such as which features to build and how to approach parts of the design. My Part 1 design and Part 2 prototype set the requirements and structure. I made the final decisions and wrote all of the code myself.

**Error handling.** When I ran into compile, runtime or test errors, I asked Claude to help me understand what the error meant and what might be causing it. I then made the fixes in my own code and tested them in Android Studio.

**Commits.** I asked Claude to help me write clear commit messages that describe the changes I had made. I also asked how to use Git commands, such as staging, committing and fixing a commit. I made and pushed every commit myself and edited the suggested messages so that they matched my actual changes.

**Documentation drafts.** Claude wrote rough drafts of some of the project's Markdown (.md) files, such as the README. I reviewed, rewrote and edited these drafts so that they accurately describe my project.

Claude did not write any of the application code.

## How I checked the work
I checked every suggestion from Claude against my own code and the official documentation before using it. I ran the Jest and JUnit suites through GitHub Actions and tested every feature on a physical Android phone. I checked each commit message against the diff before committing. I read through and edited every documentation draft to make sure it was correct.

## Example prompts
* "What does this Gradle build error mean and what could be causing it?"
* "Help me brainstorm ideas for two extra features for my fitness app."
* "Help me write a commit message for these changes: [summary of changes]."
* "Write a rough draft of a README for my project based on these notes."

## Citation
Anthropic (2026) *Claude Opus 5.5* [Large language model]. Available at: https://claude.ai (Accessed: 5 November 2026).