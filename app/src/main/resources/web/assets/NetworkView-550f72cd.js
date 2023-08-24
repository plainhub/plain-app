import{_ as x}from"./MonacoEditor.vuevuetypescriptsetuptruelang-5c83fd0a.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-8aa3c750.js";import{_ as b}from"./Breadcrumb-731a6814.js";import{d as N,u as $,r as m,i as C,t as r,a1 as p,bm as c,Y as h,o as B,c as F,b as S,e as i,g as U,M as d,ad as g}from"./index-66bea2e9.js";const q={class:"page-container container"},D={class:"main"},T=N({__name:"NetworkView",setup(M){const{t:s}=$(),o=m(0),n=m(""),t=m("");C({handle:(l,e)=>{e?r(s(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});const{mutate:_,loading:f,onDone:v}=h({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});v(()=>{r(s("saved"))});function V(){!n.value||!t.value||_({netplan:n.value,netmix:t.value})}return(l,e)=>{const w=b,k=y,u=x;return B(),F("div",q,[S("div",D,[i(w,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(k,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:V,loading:U(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(u,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(u,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{T as default};
