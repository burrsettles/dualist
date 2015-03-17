## DUALIST Changelog ##

### Version 03 - 03/08/2012 ###

 * BUG FIX: Updates to dualist.tui.Util and dualist.tui.Test that fix a bug in
   testing post-hoc trained models, in the event that the training and test
   labels are presented in a different order. (Submitted by Stef Sch)


### Version 02 - 02/10/2012 ###

 * BUG FIX: Gracefully processes non-ASCII characters.

 * BUG FIX: TwitterPipe no longer ignores text after a @USERLINK.

 * BUG FIX: Handles small toy data sets now.

 * Separated code into "core" and "gui" components. Core implements the
   machine learning business logic, whereas GUI implements the Web-based
   interactive interface. As a result, DUALIST now requires a build (using ant)
   after any changes to core.

 * Created a more user-friendly script "dualist" to run commands.

 * Models are now saved in the "models/" directory, indexed by trial name and
   timestamp. These models can be evaluated offline on totally separate test
   data, or used to classify other large data sets.