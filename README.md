PhiUtil for Java ![project status](http://stillmaintained.com/au-phiware/phi-util-java.png)
================

This project provides some utility classes to supplement the JDK:

  * CloseableBlockingQueue interface and implementation that gives the semantics
    for a [BlockingQueue][] that may be closed.
  * PausableBlockingQueue interface and implementation that gives the semantics
    for a [BlockingQueue][] that may pause or suspend the current thread.
  * Continue interface that makes it easier to coordinate paused/suspended 
    threads.


Forking
-------

Forks and pull requests are welcome.


License
-------

GNU GPL, see [http://www.gnu.org/licenses/](http://www.gnu.org/licenses/).


  [BlockingQueue]: http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/BlockingQueue.html
