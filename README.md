## DUALIST: Utility for Active Learning with Instances and Semantic Terms ##

_Hooray for recursive acronyms!_

Version 0.3 / March 08, 2012

DUALIST is an interactive machine learning system for quickly building classifiers for text processing tasks. It does so by asking "questions" of a human "teacher" in the form of both data instances (e.g., text documents) and features (e.g., words or phrases). It uses [active learning](http://www.cs.cmu.edu/~bsettles/pub/settles.activelearning.pdf) and [semi-supervised learning](http://www.cs.wisc.edu/~jerryzhu/pub/ssl_survey.pdf) to build text-based classifiers at interactive speed.

Research related to DUALIST is described in these publications:

  * B. Settles. [Closing the Loop: Fast, Interactive Semi-Supervised Annotation With Queries on Features and Instances](http://aclweb.org/anthology/D/D11/D11-1136.pdf). In _Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP)_, pages 1467-1478. ACL, 2011. ([addendum](http://www.cs.cmu.edu/~bsettles/pub/settles.emnlp11addendum.pdf))
  * B. Settles and X. Zhu. [Behavioral Factors in Interactive Training of Text Classifiers](http://www.cs.cmu.edu/~bsettles/pub/settles.naacl12short.pdf). In _Proceedings of the North American Chapter of the Association for Computational Linguistics - Human Language Technologies (NAACL HLT)_, pages 563-567. ACL, 2012.

Watch a [demonstration video](http://vimeo.com/21671958) of DUALIST in action!

----

### Purpose & Goal ###

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

See `LICENSE.txt` for licensing information.
See `CHANGELOG.md` for a history of updates.


### Installation + Running the Web-Based GUI ###

DUALIST requires Java 1.6 and Python 2.5 to work properly. It ships with most
of the dependencies it needs to work, the only exception being the Play! web
framework for Java v1.1+, which can be downloaded here:

    http://download.playframework.org/releases/play-1.1.zip

Download and install Play! wherever you want on your system (follow the
instructions on their website), and make sure that the "play" command is in
your $PATH. Once that is done, all you need to do to run DUALIST is:

    $ cd <path-to>/dualist
    $ dualist gui

This will launch a web server on your machine, which you can access by
pointing your favorite browser to:

    http://localhost:8080/

And follow the instructions on the screen. DUALIST has only been tested on Mac
OS 10.6 and Ubuntu Linux, but it should be platform-independent and work in
any unix-like environment (and even Windows). Make sure you don't have any
other processes listening on the 8080 port of your machine.

NOTE: DUALIST is written to run on a single computer and loads all data into
memory. It is robust for hundreds of thousands of instances and features on
modern hardware, but may be difficult to use beyond that.



### Logs + Output ###

DUALIST writes a log of user actions in the "results/" directory. Trained
models are archived as learning progresses in the "models/" directory. Web
server system output is written to "application.log" in the root directory.

In "Explore" mode, you can click the "predict" button at the bottom of the
page at any time to get the current model's label predictions, followed by the
set of labeled instances and features/terms (prepended by the '#' character).


### Using trained models ###

Trained models are stored in the "models/" directory. There are two utilities
for using these models to apply or evaluate these models on data:

    $ dualist classify [model] [documents...]

This takes a model file and any number of either raw-text or ZIP archive files
in the appropriate data format (see data file formats section below). DUALIST
will then output predictions to STDOUT in a tab-delimted format:

    textID  label1  prob1   label2  prob2   ... text-summary

The label predictions are output in rank order, thus column #2 corresponds to
the model's most likely prediction, and column #3 is its posterior
probability, and so on. The text summary in the final columns is a snippet of
the first 150 characters in the instance.

The other utility, for evaluation, is:

    $ dualist test [model] [test-set]

This will produce various statistics about the model and data set, as well as
the model's accuracy compared to a 10-fold cross-validation baseline using the
same test set.


### Data File Formats ###

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


### Customization ###

To create your own data processing pipelines, follow these steps:

 1. Familiarize yourself with the `cc.mallet.pipe` package API
    (http://mallet.cs.umass.edu/api/)

 2. Implement a new pipe in the `dualist.pipes` package of the DUALIST
    codebase (use `DocumentPipe.java` as an example).

 3. Edit the following files to incorporate the new pipeline into the
 web-based user interface:
  * `core/src/dualist/tui/Util.java` (the "getPipe" method)
  * `gui/app/views/Applications/experiment.html`
  * `gui/app/views/Applications/explore.html`

 4. Changes made to the `core/` section of the codebase must be manually
 compiled by typing the "ant" command. You may need to stop and restart the
 GUI in this case.

 5. Changes made to the `gui/` section of the codebase are re-compiled on
 the fly by the Play! web framework.

 6. For more advanced deployment of the web-based GUI, you will probably
 need to edit the file `gui/app/conf/application.conf`. Refer the the Play!
 documentation for more details:
 http://www.playframework.org/documentation/1.1/production

Good luck, and have fun!

---

This work is supported in part by DARPA (under contract numbers FA8750-08-1-0009 and AF8750-09-C-0179), the National Science Foundation (IIS-0968487), and Google. Any opinions, findings and conclusions or recommendations expressed in this material are the authors' and do not necessarily reflect those of the sponsors.
