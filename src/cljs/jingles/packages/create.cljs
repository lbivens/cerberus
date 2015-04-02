(ns jingles.packages.create
  (:require
   [om.core :as om :include-macros true]
   [jingles.create :as create]))

(defn render [data]
  (reify
    om/IDisplayName
    (display-name [_]
      "addpackagec")
    om/IRenderState
    (render-state [_ _]
      (create/render
       data
       {:label "Name" :id "pkg-name" :key :name}
       {:label "CPU" :unit "%" :id "pkg-cpu" :key :cpu_cap :data-type :integer :validator #(and (integer? %2) (< 0 %2))}
       {:label "Memory" :unit "MB" :id "pkg-ram" :key :ram :data-type :integer :validator #(and (integer? %2) (< 0 %2))}
       {:label "Disk" :unit "GB"  :id "pkg-quota" :key :quota :data-type :integer :validator #(and (integer? %2) (< 0 %2))}
       {:label "IO Priority" :id "pkg-iopriority" :key :iopriority :data-type :integer :optional true}
       {:label "Block Size" :unit "Byte" :id "pkg-block_size" :key :block_size :data-type :integer :optional true}
       {:label "Compression" :id "pkg-compression" :key :compression :type :select :optional true
        :options ["lz4" "lzjb" "zle" "gzip"]}))))
