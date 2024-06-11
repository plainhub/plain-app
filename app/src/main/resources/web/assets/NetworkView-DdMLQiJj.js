import{_ as k}from"./MonacoEditor.vuevuetypescriptsetuptruelang-BMlTdJ5J.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-CY1cAI1o.js";import{_ as $}from"./Breadcrumb-nQ7aa4HG.js";import{d as b,g as C,h as u,l as N,C as s,ab as p,bO as g,j as F,c as h,p as m,m as S,x as d,a_ as c,O as U,o as q}from"./index-BxNI00MG.js";const E=b({__name:"NetworkView",setup(B){const{t:i}=C(),o=u(0),n=u(""),t=u("");N({handle:(l,e)=>{e?s(i(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${g}
  `});const{mutate:_,loading:f,onDone:v}=F({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${g}
  `});v(()=>{s(i("saved"))});function w(){!n.value||!t.value||_({netplan:n.value,netmix:t.value})}return(l,e)=>{const x=$,V=y,r=k;return q(),h(U,null,[m(x,{current:()=>l.$t("page_title.network")},null,8,["current"]),m(V,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:w,loading:S(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading"]),d(m(r,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[c,o.value===0]]),d(m(r,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[c,o.value===1]])],64)}}});export{E as default};
