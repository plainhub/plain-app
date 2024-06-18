import{_ as k}from"./MonacoEditor.vuevuetypescriptsetuptruelang-DhAxQ-me.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-Cj6nFd54.js";import{_ as $}from"./Breadcrumb-nTZpptou.js";import{d as C,g as N,h as u,l as b,C as s,ac as p,bT as g,j as F,c as h,p as m,m as S,x as c,aG as d,O as U,o as q}from"./index-Dn0O6zoH.js";const E=C({__name:"NetworkView",setup(B){const{t:i}=N(),o=u(0),n=u(""),t=u("");b({handle:(l,e)=>{e?s(i(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${g}
  `});const{mutate:f,loading:_,onDone:v}=F({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${g}
  `});v(()=>{s(i("saved"))});function w(){!n.value||!t.value||f({netplan:n.value,netmix:t.value})}return(l,e)=>{const x=$,V=y,r=k;return q(),h(U,null,[m(x,{current:()=>l.$t("page_title.network")},null,8,["current"]),m(V,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:w,loading:S(_),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading"]),c(m(r,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[d,o.value===0]]),c(m(r,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[d,o.value===1]])],64)}}});export{E as default};
