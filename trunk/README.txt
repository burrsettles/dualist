DUALIST: Utility for Active Learning with Instances and Semantic Terms
======================================================================

Burr Settles
Carnegie Mellon University
bsettles@cs.cmu.edu

Version 0.1
May 25, 2011

DUALIST is an interactive machine learning system for building classifiers
quickly. It does so by asking "questions" of the user in the form of both data
instances (e.g., text documents) and features (e.g., words or phrases). It
utilizes active and semi-supervised learning to quickly train a multinomial
naive Bayes classifier for this setting.

NOTICE: This is currently "research-grade" code. It is provided AS-IS without
any warranties of any kind, expressed or implied, including but not limited to
the implied warranties of merchantability and fitness for a particular purpose
and those arising by statute or otherwise in law or from a course of dealing
or usage of trade. *Whew!*

See LICENSE.txt for licensing information.

Citation information and technical details:

	B. Settles. Closing the Loop: Fast, Interactive Semi-Supervised Annotation 
	With Queries on Features and Instances. In Proceedings of the Conference 
	on Empirical Methods in Natural Language Processing (EMNLP), to appear. 
	ACL Press, 2011.



PURPOSE & GOAL
--------------

The purpose of DUALIST is threefold:

1. A practical tool to expedite annotation/learning in NLP tasks.

2. A framework to facilitate research in interactive and multi-modal active
learning. This includes enabling actual user experiments with the GUI (as
opposed to simulated experiments, which are pervasive in the literature but
sometimes practically inconclusive) as well as developing more advanced dual
supervision strategies which are fast enough to be interactive, accurate
enough to be useful, and perhaps make more appropriate modeling assumptions
than the multinomial naive Bayes classifier currently used.

3. A starting point for more sophisticated interactive learning scenarios that
combine multiple "beyond supervised learning" strategies. This ICML workshop
is related: https://sites.google.com/site/comblearn/



INTALLATION + USAGE
-------------------

DUALIST requires Java 1.6 to work properly. It ships with most of the
libraries it needs to work, the only exception being the Play! web framework
for Java v1.1, which can be downloaded here:

	http://download.playframework.org/releases/play-1.1.zip

Download and install Play! wherever you want on your system (follow the 
instructions on their website), and make sure that the "play" command is in 
your $PATH. Once that is done, all you need to do to run DUALIST is:

	% cd <path-to>/dualist
	% play run

This will launch a web server on your machine, which you can access by 
pointing your favorite browser to:

	http://localhost:9000/

And follow the instructions on the screen. DUALIST has only been tested on Mac
OS 10.6 and Ubuntu Linux, but it should be platform-independent and work in
any unix-like environment (and even Windows). Make sure you don't have any
other processes listening on the 9000 port of your machine.

NOTE: DUALIST is written to run on a single computer and loads all data into
memory. It is robust for hundreds of thousands of instances and features on
modern hardware, but may be difficult to use beyond that. If your data causes
DUALIST to run out of memory, try starting it up with an increased Java heap
size, e.g.:

	% play run -Xmx2000m



LOGS AND OUTPUT
---------------

DUALIST writes a log of user actions in the "public/results/" directory. Web
server system output is written to "application.log" in the root directory.

In "Explore" mode, you can click the "predict" button at the bottom of the
page at any time to get the current model's label predictions, followed by the
set of labeled instances and features/terms (prepended by the '#' character).



DATA FILE FORMATS
-----------------

In either explore or experiment mode, DUALIST accepts data sets as a single
ZIP file. In "explore" mode, data files can be an arbitrary structure within
the archive, it is only required that they be zipped. You define the class
labels yourself in the setup for explore mode.

In "experiment" mode, instances must have labels which are defined by
subdirectories within the archive. For example, for a classification task with
two labels "foo" and "bar," the ZIP archive structure would look like this:

	foo/foo-file1.txt
	foo/foo-file2.txt
	...
	bar/bar-file1.txt
	bar/bar-file2.txt
	...

DUALIST comes with four built-in data processing setups:

Documents: Each document (e.g., foo-file1.txt above) is its own instance. The
default feature representation is bag-of-unigrams, lowercased, with stopwords
removed.

Simple Lines: Each line of text is an instance (thus the archive can be
composed of a single file). The feature representation is the same as
"Documents," plus bigrams.

Tweets: The same as "Simple Lines," plus features for "emoticons" :) and
twitter-specific semantics (e.g., @username, http://links, and #hashtags).

Entities: One line per instances, in tab-delimited format. The instance name
(i.e., a noun phrase) is represented by the first element of the line, and
each subsequent element is a contextual feature, represented by
"feature||value" (using "||" as a delimiter). Orthographic features (word
shape, affixes, etc.) are induced automatically.



CUSTOMIZATION
-------------

To create your own data processing pipelines, follow these steps:

	1. Familiarize yourself with the "cc.mallet.pipe" package API
	(http://mallet.cs.umass.edu/api/)
	
	2. Implement a new pipe in the "app.guts.pipes" package of the DUALIST
	codebase (use "DocumentPipe.java" as an example).
	
	3. Edit the following files to incorporate the new pipeline into the 
	web-based user interface:
		app/guts/Util.java (the "readData" method)
		app/views/Applications/experiment.html
		app/views/Applications/explore.html

Good luck, and have fun!
