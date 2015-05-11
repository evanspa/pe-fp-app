(ns pe-fp-app.apptxn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Change Log Since Application Transaction Use Cases
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fpapptxn-changelog-fetch 16)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Change Log Since-related Use Case Events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fpapptxnlog-fetchclsince-initiated                      0) ;recorded client-side
(def fpapptxnlog-fetchclsince-remote-attempted               1) ;recorded client-side
(def fpapptxnlog-fetchclsince-remote-skipped-no-conn         2) ;recorded client-side
(def fpapptxnlog-fetchclsince-remote-proc-started            3)
(def fpapptxnlog-fetchclsince-remote-proc-done-err-occurred  4)
(def fpapptxnlog-fetchclsince-remote-proc-done-success       5)
(def fpapptxnlog-fetchclsince-remote-attempt-resp-received   6) ;recorded client-side
