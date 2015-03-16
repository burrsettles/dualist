## DUALIST: Utility for Active Learning with Instances and Semantic Terms ##

_Hooray for recursive acronyms!_

DUALIST is an interactive machine learning system for quickly building classifiers for text processing tasks. It does so by asking "questions" of a human "teacher" in the form of both data instances (e.g., text documents) and features (e.g., words or phrases). It uses [active learning](http://www.cs.cmu.edu/~bsettles/pub/settles.activelearning.pdf) and [semi-supervised learning](http://www.cs.wisc.edu/~jerryzhu/pub/ssl_survey.pdf) to build text-based classifiers at interactive speed.

Research related to DUALIST is described in these publications:

  * B. Settles. [Closing the Loop: Fast, Interactive Semi-Supervised Annotation With Queries on Features and Instances](http://aclweb.org/anthology/D/D11/D11-1136.pdf). In _Proceedings of the Conference on Empirical Methods in Natural Language Processing (EMNLP)_, pages 1467-1478. ACL, 2011. ([addendum](http://www.cs.cmu.edu/~bsettles/pub/settles.emnlp11addendum.pdf))
  * B. Settles and X. Zhu. [Behavioral Factors in Interactive Training of Text Classifiers](http://www.cs.cmu.edu/~bsettles/pub/settles.naacl12short.pdf). In _Proceedings of the North American Chapter of the Association for Computational Linguistics - Human Language Technologies (NAACL HLT)_, pages 563-567. ACL, 2012.


---


Watch a [demonstration video](http://vimeo.com/21671958) of DUALIST in action:

<wiki:gadget url="http://cms.molinos.ru/g/gcVideo.xml" up\_video="http://vimeo.com/21671958" width="640" height="360"/>


---


The goals of this project are threefold:

  1. A practical tool to facilitate annotation/learning in text analysis projects.
  1. A framework to facilitate research in interactive and multi-modal active learning. This includes enabling actual user experiments with the GUI (as opposed to simulated experiments, which are pervasive in the literature but sometimes inconclusive for use in practice) and exploring HCI issues, as well as supporting new dual supervision algorithms which are fast enough to be interactive, accurate enough to be useful, and might make more appropriate modeling assumptions than [multinomial naive Bayes](http://en.wikipedia.org/wiki/Naive_Bayes_classifier) (the current underlying model).
  1. A starting point for more sophisticated interactive learning scenarios that combine multiple "beyond supervised learning" strategies. See the proceedings of the recent [ICML 2011 workshop on this topic](https://sites.google.com/site/comblearn/).


---


This work is supported in part by DARPA (under contract numbers FA8750-08-1-0009 and AF8750-09-C-0179), the National Science Foundation (IIS-0968487), and Google. Any opinions, findings and conclusions or recommendations expressed in this material are the authors' and do not necessarily reflect those of the sponsors.