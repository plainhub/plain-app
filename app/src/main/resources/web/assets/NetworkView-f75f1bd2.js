import{_ as w}from"./MonacoEditor.vuevuetypescriptsetuptruelang-b8a8a823.js";import{_ as x}from"./EditToolbar.vuevuetypescriptsetuptruelang-61ee2a33.js";import{_ as N}from"./Breadcrumb-ef080890.js";import{d as $,u as b,r as s,h as C,y as r,ac as p,by as c,i as h,c as q,a as B,q as i,k as F,n as d,aV as g,o as S}from"./index-2583f876.js";const U={class:"page-container"},D={class:"main"},T=$({__name:"NetworkView",setup(A){const{t:u}=b(),o=s(0),n=s(""),a=s("");C({handle:(l,e)=>{e?r(u(e),"error"):(n.value=l.networkConfig.netplan,a.value=l.networkConfig.netmix)},document:p`
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
  `});v(()=>{r(u("saved"))});function V(){!n.value||!a.value||_({netplan:n.value,netmix:a.value})}return(l,e)=>{const y=N,k=x,m=w;return S(),q("div",U,[B("div",D,[i(y,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(k,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=t=>o.value=t),save:V,loading:F(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(m,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=t=>n.value=t)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(m,{language:"yaml",height:"700",modelValue:a.value,"onUpdate:modelValue":e[2]||(e[2]=t=>a.value=t)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{T as default};
